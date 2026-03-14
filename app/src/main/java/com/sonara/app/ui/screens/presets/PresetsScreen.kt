package com.sonara.app.ui.screens.presets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sonara.app.ui.components.FluentCard
import com.sonara.app.ui.theme.SonaraTextSecondary

@Composable
fun PresetsScreen() {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Presets", style = MaterialTheme.typography.headlineMedium)
        FluentCard {
            Text("Sound Presets", style = MaterialTheme.typography.titleMedium)
            Text("Preset library coming soon", style = MaterialTheme.typography.bodyMedium, color = SonaraTextSecondary)
        }
    }
}
