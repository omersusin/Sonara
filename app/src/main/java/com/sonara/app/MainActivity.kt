package com.sonara.app

import android.Manifest
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
import com.sonara.app.service.SonaraService
import com.sonara.app.ui.navigation.SonaraNavigation
import com.sonara.app.ui.theme.AccentColor
import com.sonara.app.ui.theme.SonaraTheme

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) SonaraService.start(this)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        ensureNotificationPermission()
        (application as SonaraApp).audioEngine.let { if (!it.isInitialized) it.init() }

        setContent {
            val prefs = (application as SonaraApp).preferences
            val accent by prefs.accentColorFlow.collectAsState(initial = AccentColor.Amber)
            SonaraTheme(accentColor = accent) { SonaraNavigation() }
        }
    }

    override fun onResume() {
        super.onResume()
        (application as SonaraApp).audioEngine.let { if (!it.isInitialized) it.init() }
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
