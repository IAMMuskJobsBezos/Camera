package org.fossify.camera.helpers

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.ContextCompat
import org.fossify.commons.helpers.BaseConfig
import org.fossify.camera.R

/**
 * Seeds Commons' stored palette with the ElderBerry colours.
 *
 * The colors.xml overrides only cover fresh installs — BaseConfig returns whatever is already in
 * SharedPreferences otherwise — so [apply] writes them explicitly on every launch. It resolves
 * from resources rather than literals, so values-night supplies the dark palette automatically.
 *
 * The pre-ElderBerry values are backed up once, on the first run that applies the palette, and
 * [revert] restores them.
 */
object ElderBerryTheme {

    private const val BACKUP_TAKEN = "eb_theme_backup_taken"
    private const val BACKUP_TEXT = "eb_theme_backup_text_color"
    private const val BACKUP_BACKGROUND = "eb_theme_backup_background_color"
    private const val BACKUP_PRIMARY = "eb_theme_backup_primary_color"
    private const val BACKUP_ACCENT = "eb_theme_backup_accent_color"
    private const val BACKUP_SYSTEM_THEME = "eb_theme_backup_system_theme"
    private const val BACKUP_GLOBAL_THEME = "eb_theme_backup_global_theme"

    fun apply(context: Context, config: BaseConfig, prefs: SharedPreferences) {
        if (!prefs.getBoolean(BACKUP_TAKEN, false)) {
            prefs.edit()
                .putInt(BACKUP_TEXT, config.textColor)
                .putInt(BACKUP_BACKGROUND, config.backgroundColor)
                .putInt(BACKUP_PRIMARY, config.primaryColor)
                .putInt(BACKUP_ACCENT, config.accentColor)
                .putBoolean(BACKUP_SYSTEM_THEME, config.isSystemThemeEnabled)
                .putBoolean(BACKUP_GLOBAL_THEME, config.isGlobalThemeEnabled)
                .putBoolean(BACKUP_TAKEN, true)
                .apply()
        }

        // Material You and the Fossify shared-theme provider both override the stored palette,
        // so they have to be off for these writes to survive.
        config.isSystemThemeEnabled = false
        config.isGlobalThemeEnabled = false

        config.textColor = ContextCompat.getColor(context, R.color.eb_ink)
        config.backgroundColor = ContextCompat.getColor(context, R.color.eb_canvas)
        config.primaryColor = ContextCompat.getColor(context, R.color.eb_accent)
        config.accentColor = ContextCompat.getColor(context, R.color.eb_accent)
    }

    fun revert(config: BaseConfig, prefs: SharedPreferences) {
        if (!prefs.getBoolean(BACKUP_TAKEN, false)) {
            return
        }

        config.textColor = prefs.getInt(BACKUP_TEXT, config.textColor)
        config.backgroundColor = prefs.getInt(BACKUP_BACKGROUND, config.backgroundColor)
        config.primaryColor = prefs.getInt(BACKUP_PRIMARY, config.primaryColor)
        config.accentColor = prefs.getInt(BACKUP_ACCENT, config.accentColor)
        config.isSystemThemeEnabled = prefs.getBoolean(BACKUP_SYSTEM_THEME, false)
        config.isGlobalThemeEnabled = prefs.getBoolean(BACKUP_GLOBAL_THEME, false)

        prefs.edit().putBoolean(BACKUP_TAKEN, false).apply()
    }
}
