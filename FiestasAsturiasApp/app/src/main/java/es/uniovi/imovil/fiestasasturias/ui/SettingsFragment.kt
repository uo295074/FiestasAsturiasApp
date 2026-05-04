package es.uniovi.imovil.fiestasasturias.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import es.uniovi.imovil.fiestasasturias.R
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.MaterialAutoCompleteTextView

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private val languageCodes = listOf("es", "en")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val switchDarkMode = view.findViewById<SwitchMaterial>(R.id.switchDarkMode)
        val languageSelector = view.findViewById<MaterialAutoCompleteTextView>(R.id.languageSelector)

        val prefs = requireContext().getSharedPreferences(AppPreferences.PREFS, Context.MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean(AppPreferences.KEY_DARK_MODE, false)
        val currentLanguage = prefs.getString(AppPreferences.KEY_LANGUAGE, "es") ?: "es"

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

        switchDarkMode.isChecked = isDarkMode

        applyTheme(isDarkMode)

        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(AppPreferences.KEY_DARK_MODE, isChecked).apply()
            applyTheme(isChecked)
            requireActivity().recreate()
        }

        languageSelector.setOnItemClickListener { _, _, position, _ ->
            val selectedLanguage = languageCodes[position]
            prefs.edit().putString(AppPreferences.KEY_LANGUAGE, selectedLanguage).apply()
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(selectedLanguage))
            requireActivity().recreate()
        }
    }

    private fun applyTheme(dark: Boolean) {
        AppCompatDelegate.setDefaultNightMode(
            if (dark) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}
