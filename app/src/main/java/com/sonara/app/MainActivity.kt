package com.sonara.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.sonara.app.ui.navigation.SonaraNavigation
import com.sonara.app.ui.theme.AccentColor
import com.sonara.app.ui.theme.SonaraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val prefs = (application as SonaraApp).preferences
            val accent by prefs.accentColorFlow.collectAsState(initial = AccentColor.Amber)
            SonaraTheme(accentColor = accent) {
                SonaraNavigation()
            }
        }
    }
}
