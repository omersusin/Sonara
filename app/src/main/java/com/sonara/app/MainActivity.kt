/*
 * Sonara - AI-powered audio equalizer
 * Copyright (C) 2024-2026 Sonara
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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

    override fun onResume() {
        super.onResume()
        val app = application as SonaraApp
        // Always restore connection if session key exists
        app.lastFmAuth.ensureConnectedState()
        val authState = app.lastFmAuth.authState.value
        if (authState == LastFmAuthManager.AuthState.AUTHENTICATING ||
            authState == LastFmAuthManager.AuthState.DISCONNECTED) {
            MainScope().launch {
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
