package es.uniovi.imovil.fiestasasturias.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.appbar.MaterialToolbar
import es.uniovi.imovil.fiestasasturias.R
import es.uniovi.imovil.fiestasasturias.domain.FiestaViewModel

class MainActivity : AppCompatActivity() {

    private val viewModel: FiestaViewModel by viewModels()
    private var selectedNavItemId: Int = R.id.nav_home
    private lateinit var bottomNav: BottomNavigationView

    companion object {
        private const val KEY_SELECTED_NAV = "selected_nav"
    }

    // gestionamos permiso de ubicación con el launcher moderno para evitar onActivityResult legado.
    private val locationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->

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

        selectedNavItemId = savedInstanceState?.getInt(KEY_SELECTED_NAV, R.id.nav_home) ?: R.id.nav_home

        bottomNav = findViewById(R.id.bottomNav)
        val topBar = findViewById<MaterialToolbar>(R.id.topBar)

        setSupportActionBar(topBar)

        // cargamos home solo en primer arranque; en recreaciones respetamos el fragment actual.
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, HomeFragment())
                .commit()
        }

        bottomNav.setOnItemSelectedListener {
            selectedNavItemId = it.itemId
            navigateTo(it.itemId)
        }

        if (savedInstanceState != null) {
            bottomNav.selectedItemId = selectedNavItemId
        }

        // pedimos permiso de ubicación para habilitar el filtro por km y la posición en mapa.
        checkLocationPermission()

        // evitamos recargar si el viewmodel ya conserva datos de una instancia previa.
        if (viewModel.fiestas.value == null) {
            viewModel.cargarFiestas()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(KEY_SELECTED_NAV, selectedNavItemId)
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        fetchUserLocation()
    }

    fun openHistoryFromSettings() {
        // historial cuelga de ajustes, por eso se abre como navegación secundaria con backstack.
        selectedNavItemId = R.id.nav_settings
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.fragmentContainerView, HistorialFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun navigateTo(itemId: Int): Boolean {
        val fragment = when (itemId) {
            R.id.nav_home -> HomeFragment()
            R.id.nav_list -> ListFragment()
            R.id.nav_map -> MapFragment()
            R.id.nav_fav -> FavoritosFragment()
            R.id.nav_settings -> SettingsFragment()
            else -> null
        }

        fragment?.let {
            supportFragmentManager.beginTransaction()
                .setCustomAnimations(
                    android.R.anim.fade_in,
                    android.R.anim.fade_out
                )
                .replace(R.id.fragmentContainerView, it)
                .commit()
            return true
        }
        return false
    }

    private fun checkLocationPermission() {

        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                fetchUserLocation()
            }

            else -> {
                //  lanzar petición
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
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
