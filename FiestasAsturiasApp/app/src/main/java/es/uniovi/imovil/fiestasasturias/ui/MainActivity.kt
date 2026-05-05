package es.uniovi.imovil.fiestasasturias.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import es.uniovi.imovil.fiestasasturias.R
import es.uniovi.imovil.fiestasasturias.domain.FiestaViewModel

class MainActivity : AppCompatActivity() {

    private val viewModel: FiestaViewModel by viewModels()
    private var selectedNavItemId: Int = R.id.nav_home

    companion object {
        private const val KEY_SELECTED_NAV = "selected_nav"
    }

    //  API MODERNA DE PERMISOS
    private val locationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->

            if (isGranted) {
                //  permiso concedido → recargamos para inicializar ubicación
                recreate()
            } else {
                //  permiso denegado (puedes mostrar mensaje si quieres)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppPreferences.apply(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        selectedNavItemId = savedInstanceState?.getInt(KEY_SELECTED_NAV, R.id.nav_home) ?: R.id.nav_home

        // 🔥 Fragment inicial
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, HomeFragment())
                .commit()
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        bottomNav.setOnItemSelectedListener {
            selectedNavItemId = it.itemId

            val fragment = when (it.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_list -> ListFragment()
                R.id.nav_map -> MapFragment()
                R.id.nav_fav -> FavoritosFragment()
                R.id.nav_history -> HistorialFragment()
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
            }

            true
        }

        if (savedInstanceState != null) {
            bottomNav.selectedItemId = selectedNavItemId
        }

        // 📍 PEDIR PERMISO (API NUEVA)
        checkLocationPermission()

        // 🔥 cargar datos
        if (viewModel.fiestas.value == null) {
            viewModel.cargarFiestas()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(KEY_SELECTED_NAV, selectedNavItemId)
        super.onSaveInstanceState(outState)
    }

    private fun checkLocationPermission() {

        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // ya concedido → no hacemos nada
            }

            else -> {
                //  lanzar petición
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }
}
