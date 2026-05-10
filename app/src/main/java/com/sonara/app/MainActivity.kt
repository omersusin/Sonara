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
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.sonara.app.intelligence.lastfm.LastFmAuthManager
import com.sonara.app.service.SonaraService
import com.sonara.app.ui.common.CompositionLocals
import com.sonara.app.ui.common.LocalSeedColor
import com.sonara.app.ui.domain.provider.SeedColorProvider
import com.sonara.app.ui.navigation.SonaraNavigation
import com.sonara.app.ui.theme.SonaraTheme
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
            CompositionLocals {
                SeedColorProvider.setSeedColor(LocalSeedColor.current)

                SonaraTheme {
                    SonaraNavigation()
                }
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
            authState == LastFmAuthManager.AuthState.DISCONNECTED
        ) {
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
        if (Build.VERSION.SDK_INT < 33) {
            SonaraService.start(this); return
        }
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        )
            SonaraService.start(this)
        else notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
