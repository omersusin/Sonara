package com.sonara.app.ui.screens.settings

import android.content.ComponentName
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sonara.app.service.SonaraNotificationListener
import com.sonara.app.ui.components.FluentCard
import com.sonara.app.ui.theme.SonaraSuccess
import com.sonara.app.ui.theme.SonaraTextSecondary
import com.sonara.app.ui.theme.SonaraTextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(onBack: () -> Unit = {}) {
    val ctx = LocalContext.current
    val notifEnabled = SonaraNotificationListener.isEnabled(ctx)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy & Permissions") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                FluentCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Rounded.MusicNote, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Notification Listener", style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (notifEnabled) "Granted" else "Not granted",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (notifEnabled) SonaraSuccess else MaterialTheme.colorScheme.error
                            )
                        }
                        if (notifEnabled) {
                            Icon(Icons.Rounded.CheckCircle, null, tint = SonaraSuccess, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.3f))
                    Spacer(Modifier.height(10.dp))
                    PrivacyBullet(Icons.Rounded.MusicNote, "What Sonara reads", "Song title, artist, album name, playback state, and track duration from active media sessions.")
                    Spacer(Modifier.height(8.dp))
                    PrivacyBullet(Icons.Rounded.NotificationsOff, "What Sonara ignores", "All notification content: messages, text, app names, and any non-music data are never read or stored.")
                    Spacer(Modifier.height(8.dp))
                    PrivacyBullet(Icons.Rounded.Storage, "Where data goes", "Music metadata stays on-device. Only anonymous audio feature vectors may be shared if Community Contribute is enabled.")
                    Spacer(Modifier.height(8.dp))
                    PrivacyBullet(Icons.Rounded.Lock, "API keys", "All keys (Last.fm, Gemini, etc.) are stored in Android's EncryptedSharedPreferences and never sent to Sonara servers.")
                    if (!notifEnabled) {
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Open Notification Settings") }
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun PrivacyBullet(icon: ImageVector, title: String, body: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp).padding(top = 2.dp))
        Column {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(body, style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
        }
    }
}
