package com.sonara.app.ui.screens.settings

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
fun SettingsScreen() {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        FluentCard {
            Text("Last.fm Configuration", style = MaterialTheme.typography.titleMedium)
            Text("API key setup coming soon", style = MaterialTheme.typography.bodyMedium, color = SonaraTextSecondary)
        }
    }
}
