package es.uniovi.imovil.fiestasasturias.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import es.uniovi.imovil.fiestasasturias.R
import es.uniovi.imovil.fiestasasturias.domain.FiestaViewModel

class MainActivity : AppCompatActivity() {

    private val viewModel: FiestaViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 🔥 Fragment inicial (SIEMPRE igual en móvil y tablet)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, HomeFragment())
                .commit()
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        bottomNav.setOnItemSelectedListener {

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

        // 📍 Permisos ubicación (solo aquí, correcto)
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1001
            )
        }

        viewModel.cargarFiestas()
    }
}