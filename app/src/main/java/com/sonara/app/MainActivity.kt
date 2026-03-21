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

/**
 * Madde 8 FIX: Deep link callback işleniyor (sonara://lastfm-auth)
 * Madde 9 FIX: Theme ayarları (themeMode, dynamicColors, highContrast) SonaraTheme'e geçiriliyor
 */
class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) SonaraService.start(this)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ensureNotificationPermission()

        // Madde 8: Handle deep link if launched via intent
        handleLastFmDeepLink(intent)

        setContent {
            val prefs = (application as SonaraApp).preferences
            val accent by prefs.accentColorFlow.collectAsState(initial = AccentColor.Amber)
            val themeMode by prefs.themeModeFlow.collectAsState(initial = "dark")
            val dynamicColors by prefs.dynamicColorsFlow.collectAsState(initial = false)
            val highContrast by prefs.highContrastFlow.collectAsState(initial = false)
            val amoledMode by prefs.amoledModeFlow.collectAsState(initial = false)

            SonaraTheme(
                accentColor = accent,
                themeMode = themeMode,
                dynamicColors = dynamicColors,
                highContrast = highContrast,
                amoledMode = amoledMode
            ) {
                SonaraNavigation()
            }
        }
    }

    /**
     * Madde 8 FIX: Deep link callback — Last.fm OAuth dönüşünü işle
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleLastFmDeepLink(intent)
    }

    private fun handleLastFmDeepLink(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == LastFmAuthManager.CALLBACK_SCHEME && uri.host == LastFmAuthManager.CALLBACK_HOST) {
            SonaraLogger.i("MainActivity", "Last.fm auth callback received")
            val token = uri.getQueryParameter("token")
            val app = application as SonaraApp
            MainScope().launch {
                val success = app.lastFmAuth.handleCallback(token)
                if (success) {
                    SonaraLogger.i("MainActivity", "Last.fm auth successful")
                    app.reloadPipeline()
                } else {
                    SonaraLogger.w("MainActivity", "Last.fm auth failed")
                }
            }
        }
    }

    private fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT < 33) {
            SonaraService.start(this)
            return
        }
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            SonaraService.start(this)
        } else {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
