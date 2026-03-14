package com.sonara.app.ui.screens.equalizer

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
fun EqualizerScreen() {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Equalizer", style = MaterialTheme.typography.headlineMedium)
        FluentCard {
            Text("10-Band Equalizer", style = MaterialTheme.typography.titleMedium)
            Text("Full EQ controls coming soon", style = MaterialTheme.typography.bodyMedium, color = SonaraTextSecondary)
        }
    }
}
