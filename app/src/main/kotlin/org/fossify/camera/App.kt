package org.fossify.camera

import android.content.res.Configuration
import org.fossify.commons.FossifyApp
import org.fossify.camera.extensions.config

class App : FossifyApp() {
    override fun onCreate() {
        super.onCreate()
        config.applyElderBerryTheme()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Re-resolve the palette so a light/dark switch is picked up without a relaunch.
        config.applyElderBerryTheme()
    }
}
