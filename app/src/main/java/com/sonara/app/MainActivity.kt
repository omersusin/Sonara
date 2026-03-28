package com.sonara.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import com.sonara.app.data.SonaraLogger
import com.sonara.app.intelligence.lastfm.LastFmAuthManager
import com.sonara.app.service.SonaraService
import com.sonara.app.ui.navigation.SonaraNavigation
import com.sonara.app.ui.theme.AccentColor
import com.sonara.app.ui.theme.SonaraTheme
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) SonaraService.start(this)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ensureNotificationPermission()
        handleLastFmDeepLink(intent)

        setContent {
            val prefs = (application as SonaraApp).preferences
            val accent by prefs.accentColorFlow.collectAsState(initial = AccentColor.Amber)
            val themeMode by prefs.themeModeFlow.collectAsState(initial = "dark")
            val dynamicColors by prefs.dynamicColorsFlow.collectAsState(initial = false)
            val highContrast by prefs.highContrastFlow.collectAsState(initial = false)
            val amoledMode by prefs.amoledModeFlow.collectAsState(initial = false)

            SonaraTheme(accentColor = accent, themeMode = themeMode, dynamicColors = dynamicColors,
                highContrast = highContrast, amoledMode = amoledMode) {
                SonaraNavigation()
            }
        }
    }

    // FIX: onResume artik gereksiz handleCallback CAGIRMIYOR
    override fun onResume() {
        super.onResume()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleLastFmDeepLink(intent)
    }

    private fun handleLastFmDeepLink(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == LastFmAuthManager.CALLBACK_SCHEME && uri.host == LastFmAuthManager.CALLBACK_HOST) {
            val token = uri.getQueryParameter("token")
            val app = application as SonaraApp
            MainScope().launch {
                val success = app.lastFmAuth.handleCallback(token)
                if (success) app.reloadPipeline()
            }
        }
    }

    private fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT < 33) { SonaraService.start(this); return }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
            SonaraService.start(this)
        else notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
