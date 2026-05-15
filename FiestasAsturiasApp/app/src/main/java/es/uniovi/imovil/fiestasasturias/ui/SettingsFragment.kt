package es.uniovi.imovil.fiestasasturias.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import es.uniovi.imovil.fiestasasturias.R
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.button.MaterialButton
import es.uniovi.imovil.fiestasasturias.domain.FiestaViewModel

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    // índice 0 y 1 corresponden al orden de etiquetas del selector.
    private val languageCodes = listOf("es", "en")
    private val mapTypeCodes = listOf(AppPreferences.MAP_TYPE_NORMAL, AppPreferences.MAP_TYPE_SATELLITE)
    private val viewModel: FiestaViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val switchDarkMode = view.findViewById<SwitchMaterial>(R.id.switchDarkMode)
        val languageSelector = view.findViewById<MaterialAutoCompleteTextView>(R.id.languageSelector)
        val mapTypeSelector = view.findViewById<MaterialAutoCompleteTextView>(R.id.mapTypeSelector)
        val btnOpenHistory = view.findViewById<MaterialButton>(R.id.btnOpenHistory)
        val btnClearHistory = view.findViewById<MaterialButton>(R.id.btnClearHistory)
        val btnClearFavorites = view.findViewById<MaterialButton>(R.id.btnClearFavorites)

        val prefs = requireContext().getSharedPreferences(AppPreferences.PREFS, Context.MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean(AppPreferences.KEY_DARK_MODE, false)
        val currentLanguage = prefs.getString(AppPreferences.KEY_LANGUAGE, "es") ?: "es"
        val currentMapType = prefs.getString(AppPreferences.KEY_MAP_TYPE, AppPreferences.DEFAULT_MAP_TYPE)
            ?: AppPreferences.DEFAULT_MAP_TYPE

        val languageLabels = listOf(
            getString(R.string.language_spanish),
            getString(R.string.language_english)
        )

        languageSelector.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, languageLabels)
        )
        languageSelector.setText(
            languageLabels[languageCodes.indexOf(currentLanguage).coerceAtLeast(0)],
            false
        )

        val mapTypeLabels = listOf(
            getString(R.string.map_type_normal),
            getString(R.string.map_type_satellite)
        )
        mapTypeSelector.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, mapTypeLabels)
        )
        mapTypeSelector.setText(
            mapTypeLabels[mapTypeCodes.indexOf(currentMapType).coerceAtLeast(0)],
            false
        )

        switchDarkMode.isChecked = isDarkMode

        // aplicamos el tema actual por si se llega a ajustes tras recreaciones
        applyTheme(isDarkMode)

        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(AppPreferences.KEY_DARK_MODE, isChecked).apply()
            applyTheme(isChecked)
            // recreamos para que el cambio visual impacte en toda la activity
            requireActivity().recreate()
        }

        languageSelector.setOnItemClickListener { _, _, position, _ ->
            val selectedLanguage = languageCodes[position]
            prefs.edit().putString(AppPreferences.KEY_LANGUAGE, selectedLanguage).apply()
            // setApplicationLocales ya fuerza la actualización de la activity
            // si ademas llamamos a recreate(), en algunos dispositivos se nota un parpadeo extra
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(selectedLanguage))
        }

        mapTypeSelector.setOnItemClickListener { _, _, position, _ ->
            val selectedMapType = mapTypeCodes[position]
            // no hace falta recrear activity, el mapa aplicara este ajuste cuando se abra
            prefs.edit().putString(AppPreferences.KEY_MAP_TYPE, selectedMapType).apply()
        }

        btnOpenHistory.setOnClickListener {
            // historial cuelga de ajustes como navegación secundaria
            (activity as? MainActivity)?.openHistoryFromSettings()
        }

        btnClearHistory.setOnClickListener {
            // vacia historial local y actualiza la ui sin reiniciar la app
            viewModel.clearHistorial()
            Toast.makeText(requireContext(), getString(R.string.clear_history), Toast.LENGTH_SHORT).show()
        }

        btnClearFavorites.setOnClickListener {
            // quita todos los favoritos guardados y refresca lista/favoritos al instante
            viewModel.clearFavoritos()
            Toast.makeText(requireContext(), getString(R.string.clear_favorites), Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyTheme(dark: Boolean) {
        AppCompatDelegate.setDefaultNightMode(
            if (dark) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}
