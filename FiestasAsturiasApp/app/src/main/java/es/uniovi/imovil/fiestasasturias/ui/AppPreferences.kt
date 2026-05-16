package es.uniovi.imovil.fiestasasturias.ui

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object AppPreferences {
    const val PREFS = "settings_prefs"
    const val KEY_DARK_MODE = "dark_mode"
    const val KEY_LANGUAGE = "language"
    const val KEY_MAP_TYPE = "map_type"
    const val KEY_LOCATION_PERMISSION_ASKED = "location_permission_asked"
    const val MAP_TYPE_NORMAL = "normal"
    const val MAP_TYPE_SATELLITE = "satellite"
    const val DEFAULT_MAP_TYPE = MAP_TYPE_NORMAL

    fun apply(context: Context) {
        // centralizamos aquí la lectura de preferencias para que la activity solo llame a un punto.
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val darkMode = prefs.getBoolean(KEY_DARK_MODE, false)
        val language = prefs.getString(KEY_LANGUAGE, null)

        AppCompatDelegate.setDefaultNightMode(
            if (darkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )

        // si el usuario no eligió idioma todavía, respetamos el del dispositivo.
        if (!language.isNullOrBlank()) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language))
        }
    }
}
