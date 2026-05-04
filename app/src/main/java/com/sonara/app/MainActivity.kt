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
import androidx.compose.ui.graphics.Color
import com.sonara.app.ui.theme.AccentSeeds
import com.sonara.app.ui.theme.SonaraFont
import com.sonara.app.ui.theme.SonaraPaletteStyle
import com.sonara.app.ui.theme.SonaraTheme
import androidx.lifecycle.lifecycleScope
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
            val accentSeed by prefs.accentSeedFlow.collectAsState(initial = AccentSeeds.Amber.seed)
            val themeMode by prefs.themeModeFlow.collectAsState(initial = "dark")
            val dynamicColor by prefs.dynamicColorsFlow.collectAsState(initial = false)
            val highContrast by prefs.highContrastFlow.collectAsState(initial = false)
            val amoledMode by prefs.amoledModeFlow.collectAsState(initial = false)
            val selectedFont by prefs.selectedFontFlow.collectAsState(initial = "INTER")
            val selectedPaletteStyle by prefs.selectedPaletteStyleFlow.collectAsState(initial = "EXPRESSIVE")

            SonaraTheme(
                seedColor = accentSeed,
                themeMode = themeMode,
                dynamicColor = dynamicColor,
                highContrast = highContrast,
                isAmoled = amoledMode,
                font = SonaraFont.fromId(selectedFont),
                paletteStyle = SonaraPaletteStyle.fromId(selectedPaletteStyle)
            ) {
                SonaraNavigation()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val app = application as SonaraApp
        // Always restore connection if session key exists
        app.lastFmAuth.ensureConnectedState()
        val authState = app.lastFmAuth.authState.value
        if (authState == LastFmAuthManager.AuthState.AUTHENTICATING ||
            authState == LastFmAuthManager.AuthState.DISCONNECTED) {
            lifecycleScope.launch {
                if (app.lastFmAuth.hasPendingAuth()) {
                    val success = app.lastFmAuth.handleCallback()
                    if (success) app.reloadPipeline()
                }
            }
        }
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
            lifecycleScope.launch {
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
