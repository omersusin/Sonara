package com.sonara.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sonara.app.ui.theme.SonaraTheme
import com.sonara.app.ui.navigation.SonaraNavigation

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SonaraTheme {
                SonaraNavigation()
            }
        }
    }
}
