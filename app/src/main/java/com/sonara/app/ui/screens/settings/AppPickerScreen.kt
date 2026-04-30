package com.sonara.app.ui.screens.settings

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.media.session.MediaSessionManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sonara.app.SonaraApp
import com.sonara.app.ui.theme.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class AppItem(val pkg: String, val label: String, val isMusic: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val app = SonaraApp.instance
    var searchQuery by remember { mutableStateOf("") }
    var allowedApps by remember { mutableStateOf(emptySet<String>()) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { allowedApps = app.preferences.allowedScrobbleAppsFlow.first() }

    val allApps = remember {
        val pm = ctx.packageManager
        val musicPkgs = setOf("com.spotify.music", "com.google.android.apps.youtube.music", "com.apple.android.music",
            "deezer.android.app", "com.soundcloud.android", "com.amazon.mp3", "com.aspiro.tidal",
            "com.maxmpz.audioplayer", "org.videolan.vlc", "com.samsung.android.app.soundpicker",
            "com.anker.soundcore", "app.revanced.android.apps.youtube.music", "com.pandora.android",
            "com.shazam.android", "com.jrtstudio.AnotherMusicPlayer", "com.neutroncode.mp")
        val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 || it.packageName in musicPkgs }
            .map { AppItem(it.packageName, pm.getApplicationLabel(it).toString(), it.packageName in musicPkgs) }
            .sortedWith(compareByDescending<AppItem> { it.isMusic }.thenBy { it.label.lowercase() })
        installed
    }

    val filtered = if (searchQuery.isBlank()) allApps else allApps.filter {
        it.label.contains(searchQuery, ignoreCase = true) || it.pkg.contains(searchQuery, ignoreCase = true)
    }
    val musicApps = filtered.filter { it.isMusic }
    val otherApps = filtered.filter { !it.isMusic }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Choose apps") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
            item {
                OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it },
                    placeholder = { Text("Search", color = SonaraTextTertiary) },
                    leadingIcon = { Icon(Icons.Rounded.Search, null, tint = SonaraTextTertiary) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    singleLine = true, shape = MaterialTheme.shapes.large,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = SonaraDivider))
            }

            if (musicApps.isNotEmpty()) {
                item { SectionLabel("Music Players") }
                items(musicApps, key = { it.pkg }) { appItem ->
                    AppRow(appItem, appItem.pkg in allowedApps) { checked ->
                        allowedApps = if (checked) allowedApps + appItem.pkg else allowedApps - appItem.pkg
                        scope.launch { app.preferences.setAllowedScrobbleApps(allowedApps) }
                    }
                }
            }
            if (otherApps.isNotEmpty()) {
                item { SectionLabel("Other Apps") }
                items(otherApps, key = { it.pkg }) { appItem ->
                    AppRow(appItem, appItem.pkg in allowedApps) { checked ->
                        allowedApps = if (checked) allowedApps + appItem.pkg else allowedApps - appItem.pkg
                        scope.launch { app.preferences.setAllowedScrobbleApps(allowedApps) }
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Surface(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = SonaraTextSecondary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp))
    }
}

@Composable
private fun AppRow(item: AppItem, isSelected: Boolean, onToggle: (Boolean) -> Unit) {
    val p = MaterialTheme.colorScheme.primary
    val bgColor = if (isSelected) p.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
    Surface(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), shape = MaterialTheme.shapes.medium, color = bgColor) {
        Row(modifier = Modifier.clickable { onToggle(!isSelected) }.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(item.label, style = MaterialTheme.typography.bodyLarge, color = SonaraTextPrimary)
            }
            if (isSelected) {
                Surface(modifier = Modifier.size(28.dp), shape = MaterialTheme.shapes.small, color = p) {
                    Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(4.dp))
                }
            } else {
                Surface(modifier = Modifier.size(28.dp), shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh, content = {})
            }
        }
    }
}
