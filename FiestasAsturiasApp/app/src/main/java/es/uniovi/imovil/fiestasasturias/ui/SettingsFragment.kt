package es.uniovi.imovil.fiestasasturias.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import es.uniovi.imovil.fiestasasturias.R
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.button.MaterialButton
import es.uniovi.imovil.fiestasasturias.domain.FiestaViewModel

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    // indice 0 y 1 corresponden al orden de etiquetas del selector.
    private val languageCodes = listOf("es", "en")
    private val mapTypeCodes = listOf(AppPreferences.MAP_TYPE_NORMAL, AppPreferences.MAP_TYPE_SATELLITE)
    private val viewModel: FiestaViewModel by activityViewModels()
    private var btnLocationPermission: MaterialButton? = null

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (granted) {
            (activity as? MainActivity)?.refreshLocationFromSettings()
            updateLocationButtonState()
            Toast.makeText(requireContext(), getString(R.string.location_permission_granted), Toast.LENGTH_SHORT).show()
        } else {
            showLocationSettingsDialog()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val switchDarkMode = view.findViewById<SwitchMaterial>(R.id.switchDarkMode)
        val languageSelector = view.findViewById<MaterialAutoCompleteTextView>(R.id.languageSelector)
        val mapTypeSelector = view.findViewById<MaterialAutoCompleteTextView>(R.id.mapTypeSelector)
        val btnOpenHistory = view.findViewById<MaterialButton>(R.id.btnOpenHistory)
        val btnClearHistory = view.findViewById<MaterialButton>(R.id.btnClearHistory)
        val btnClearFavorites = view.findViewById<MaterialButton>(R.id.btnClearFavorites)
        btnLocationPermission = view.findViewById(R.id.btnLocationPermission)

        val prefs = requireContext().getSharedPreferences(AppPreferences.PREFS, Context.MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean(AppPreferences.KEY_DARK_MODE, false)
        val currentLanguage = resolveCurrentLanguageCode(prefs)
        val currentMapType = prefs.getString(AppPreferences.KEY_MAP_TYPE, AppPreferences.DEFAULT_MAP_TYPE)
            ?: AppPreferences.DEFAULT_MAP_TYPE

        languageSelector.keyListener = null
        languageSelector.isFocusable = false
        languageSelector.isClickable = true
        val languageLabel = if (currentLanguage == "en") {
            getString(R.string.language_english)
        } else {
            getString(R.string.language_spanish)
        }
        languageSelector.setText(languageLabel, false)
        languageSelector.setOnClickListener {
            val options = arrayOf(
                getString(R.string.language_spanish),
                getString(R.string.language_english)
            )
            val checked = if (resolveCurrentLanguageCode(prefs) == "en") 1 else 0

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.language))
                .setSingleChoiceItems(options, checked) { dialog, which ->
                    val selectedLanguage = languageCodes[which]
                    prefs.edit().putString(AppPreferences.KEY_LANGUAGE, selectedLanguage).apply()
                    languageSelector.setText(options[which], false)
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(selectedLanguage))
                    dialog.dismiss()
                }
                .show()
        }

        mapTypeSelector.keyListener = null
        mapTypeSelector.isFocusable = false
        mapTypeSelector.isClickable = true
        val mapTypeLabel = if (currentMapType == AppPreferences.MAP_TYPE_SATELLITE) {
            getString(R.string.map_type_satellite)
        } else {
            getString(R.string.map_type_normal)
        }
        mapTypeSelector.setText(mapTypeLabel, false)
        mapTypeSelector.setOnClickListener {
            val options = arrayOf(
                getString(R.string.map_type_normal),
                getString(R.string.map_type_satellite)
            )
            val checked = if (
                prefs.getString(AppPreferences.KEY_MAP_TYPE, AppPreferences.DEFAULT_MAP_TYPE)
                == AppPreferences.MAP_TYPE_SATELLITE
            ) 1 else 0

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.map_type))
                .setSingleChoiceItems(options, checked) { dialog, which ->
                    val selectedMapType = mapTypeCodes[which]
                    prefs.edit().putString(AppPreferences.KEY_MAP_TYPE, selectedMapType).apply()
                    mapTypeSelector.setText(options[which], false)
                    dialog.dismiss()
                }
                .show()
        }

        switchDarkMode.isChecked = isDarkMode

        // aplicamos el tema actual por si se llega a ajustes tras recreaciones
        applyTheme(isDarkMode)

        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(AppPreferences.KEY_DARK_MODE, isChecked).apply()
            applyTheme(isChecked)
            // recreamos para que el cambio visual impacte en toda la activity
            requireActivity().recreate()
        }

        btnOpenHistory.setOnClickListener {
            // historial cuelga de ajustes como navegación secundaria
            (activity as? MainActivity)?.openHistoryFromSettings()
        }

        updateLocationButtonState()
        btnLocationPermission?.setOnClickListener {
            requestLocationPermissionFromSettings()
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

    override fun onResume() {
        super.onResume()
        updateLocationButtonState()
    }

    private fun applyTheme(dark: Boolean) {
        AppCompatDelegate.setDefaultNightMode(
            if (dark) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    private fun resolveCurrentLanguageCode(prefs: android.content.SharedPreferences): String {
        val stored = prefs.getString(AppPreferences.KEY_LANGUAGE, null)
        if (stored in languageCodes) return stored ?: "es"

        val appLocale = AppCompatDelegate.getApplicationLocales()[0]?.language
        if (appLocale in languageCodes) {
            prefs.edit().putString(AppPreferences.KEY_LANGUAGE, appLocale).apply()
            return appLocale ?: "es"
        }

        val configLocale = resources.configuration.locales[0]?.language
        if (configLocale in languageCodes) {
            prefs.edit().putString(AppPreferences.KEY_LANGUAGE, configLocale).apply()
            return configLocale ?: "es"
        }

        return "es"
    }

    private fun requestLocationPermissionFromSettings() {
        if (hasLocationPermission()) {
            updateLocationButtonState()
            return
        }

        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun hasLocationPermission(): Boolean {
        val context = requireContext()
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun showLocationSettingsDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.location_permission_title))
            .setMessage(getString(R.string.location_permission_denied_message))
            .setPositiveButton(getString(R.string.open_app_settings)) { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", requireContext().packageName, null)
                }
                startActivity(intent)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun updateLocationButtonState() {
        val button = btnLocationPermission ?: return
        val granted = hasLocationPermission()

        button.isEnabled = !granted
        button.alpha = if (granted) 0.55f else 1f
        button.text = if (granted) {
            getString(R.string.location_permission_enabled)
        } else {
            getString(R.string.location_permission_button)
        }
    }
}
