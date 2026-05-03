package es.uniovi.imovil.fiestasasturias.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Switch
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import es.uniovi.imovil.fiestasasturias.R
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private val PREFS = "settings_prefs"
    private val KEY_DARK_MODE = "dark_mode"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val switchDarkMode = view.findViewById<SwitchMaterial>(R.id.switchDarkMode)

        val prefs = requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean(KEY_DARK_MODE, false)

        // estado inicial
        switchDarkMode.isChecked = isDarkMode

        aplicarModo(isDarkMode)

        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->

            // guardar preferencia
            prefs.edit().putBoolean(KEY_DARK_MODE, isChecked).apply()

            aplicarModo(isChecked)
            requireActivity().recreate()
        }
    }

    private fun aplicarModo(dark: Boolean) {
        AppCompatDelegate.setDefaultNightMode(
            if (dark) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}