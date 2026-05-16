package es.uniovi.imovil.fiestasasturias.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.appbar.MaterialToolbar
import es.uniovi.imovil.fiestasasturias.R
import es.uniovi.imovil.fiestasasturias.domain.FiestaViewModel

class MainActivity : AppCompatActivity() {

    private val viewModel: FiestaViewModel by viewModels()
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    // gestionamos permiso de ubicación con el launcher moderno para evitar onActivityResult legado.
    private val locationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->

            getSharedPreferences(AppPreferences.PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(AppPreferences.KEY_LOCATION_PERMISSION_ASKED, true)
                .apply()

            if (isGranted) {
                fetchUserLocation()
            } else {
                // si no hay permiso, la app sigue funcionando sin filtro por distancia.
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        // aplicamos tema e idioma antes de inflar vistas para que el arranque sea consistente.
        AppPreferences.apply(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottomNav)
        val topBar = findViewById<MaterialToolbar>(R.id.topBar)

        setSupportActionBar(topBar)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_home, R.id.nav_list, R.id.nav_map, R.id.nav_fav, R.id.nav_settings)
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        bottomNav.setupWithNavController(navController)

        // pedimos permiso de ubicación para habilitar el filtro por km y la posición en mapa.
        checkLocationPermission()

        // evitamos recargar si el viewmodel ya conserva datos de una instancia previa.
        if (viewModel.fiestas.value == null) {
            viewModel.cargarFiestas()
        }
    }

    override fun onResume() {
        super.onResume()
        fetchUserLocation()
    }

    fun openHistoryFromSettings() {
        if (navController.currentDestination?.id != R.id.nav_history) {
            navController.navigate(R.id.nav_history)
        }
    }

    fun refreshLocationFromSettings() {
        fetchUserLocation()
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun checkLocationPermission() {

        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                fetchUserLocation()
            }

            !hasAskedLocationPermission() -> {
                // solo pedimos ubicación automáticamente una vez; después queda el botón de ajustes.
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun hasAskedLocationPermission(): Boolean {
        return getSharedPreferences(AppPreferences.PREFS, Context.MODE_PRIVATE)
            .getBoolean(AppPreferences.KEY_LOCATION_PERMISSION_ASKED, false)
    }

    private fun fetchUserLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // primero intentamos última ubicación conocida por rapidez.
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                viewModel.setUserLocation(location.latitude, location.longitude)
            }
        }

        // luego pedimos una ubicación fresca para mejorar precisión.
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            null
        ).addOnSuccessListener { location ->
            if (location != null) {
                viewModel.setUserLocation(location.latitude, location.longitude)
            }
        }
    }
}
