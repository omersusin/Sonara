package com.sonara.app.ui.screens.share

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonara.app.intelligence.lyrics.LyricLine

enum class ShareCardTheme(val label: String) {
    MINIMAL("Minimal"), QUOTE("Quote"), GRADIENT("Gradient"), SPOTIFY("Spotify")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsShareScreen(
    title: String, artist: String, lines: List<LyricLine>,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    onBack: () -> Unit = {}
) {
    val ctx = LocalContext.current
    val p = MaterialTheme.colorScheme.primary
    val sel = remember { mutableStateListOf<Int>() }
    var theme by remember { mutableStateOf(ShareCardTheme.MINIMAL) }

    fun doShare() {
        if (sel.isEmpty()) return
        val text = sel.sorted().mapNotNull { lines.getOrNull(it)?.text }.joinToString("\n")
        ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "\"$text\"\n\n$title — $artist\n#Sonara")
        }, "Share lyrics"))
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Share Lyrics") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back") } },
            actions = { IconButton(onClick = ::doShare) {
                Icon(Icons.Rounded.Share, "Share", tint = if (sel.isEmpty()) MaterialTheme.colorScheme.outline else p)
            } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent))
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                if (artist.isNotBlank()) Text(artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(if (sel.isEmpty()) "Tap lines to select" else "${sel.size} line(s) selected",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (sel.isEmpty()) MaterialTheme.colorScheme.outline else p)
            }

            LazyRow(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ShareCardTheme.entries) { t ->
                    FilterChip(selected = theme == t, onClick = { theme = t },
                        label = { Text(t.label, style = MaterialTheme.typography.labelSmall) })
                }
            }

            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                itemsIndexed(lines, key = { idx, _ -> idx }) { idx, line ->
                    val selected = idx in sel
                    Box(Modifier.fillMaxWidth()
                        .background(if (selected) p.copy(0.15f) else Color.Transparent, RoundedCornerShape(8.dp))
                        .clickable { if (selected) sel.remove(idx) else sel.add(idx) }
                        .padding(horizontal = 12.dp, vertical = 10.dp)) {
                        Text(line.text, style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal),
                            color = if (selected) p else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            if (sel.isNotEmpty()) {
                val selectedTexts = sel.sorted().mapNotNull { lines.getOrNull(it)?.text }
                Box(Modifier.fillMaxWidth().padding(16.dp)) {
                    when (theme) {
                        ShareCardTheme.MINIMAL -> Box(Modifier.fillMaxWidth().background(Color.Black, RoundedCornerShape(16.dp)).padding(24.dp)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                selectedTexts.forEach { Text(it, color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
                                Spacer(Modifier.height(12.dp))
                                Text("$title — $artist", color = Color.White.copy(0.5f), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        ShareCardTheme.QUOTE -> Box(Modifier.fillMaxWidth().background(accentColor.copy(0.1f), RoundedCornerShape(16.dp))
                            .border(1.dp, accentColor.copy(0.3f), RoundedCornerShape(16.dp)).padding(24.dp)) {
                            Column {
                                Text("“", fontSize = 48.sp, color = accentColor.copy(0.4f))
                                selectedTexts.forEach { Text(it, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) }
                                Spacer(Modifier.height(8.dp))
                                Text("— $title, $artist", color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        ShareCardTheme.GRADIENT -> Box(Modifier.fillMaxWidth()
                            .background(Brush.linearGradient(listOf(accentColor.copy(0.8f), accentColor.copy(0.2f))), RoundedCornerShape(16.dp)).padding(24.dp)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                selectedTexts.forEach { Text(it, color = Color.White, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, style = MaterialTheme.typography.titleLarge) }
                                Spacer(Modifier.height(16.dp))
                                Text("$title • $artist", color = Color.White.copy(0.7f), style = MaterialTheme.typography.labelMedium)
                                Text("Sonara", color = Color.White.copy(0.4f), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
                            }
                        }
                        ShareCardTheme.SPOTIFY -> Box(Modifier.fillMaxWidth().background(Color(0xFF121212), RoundedCornerShape(16.dp)).padding(24.dp)) {
                            Column {
                                selectedTexts.forEach { Text(it, color = Color.White, fontWeight = FontWeight.Bold) }
                                Spacer(Modifier.height(16.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(8.dp).background(Color(0xFF1DB954), CircleShape))
                                    Spacer(Modifier.width(8.dp))
                                    Text("$title — $artist", color = Color.White.copy(0.7f), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }

            if (sel.isNotEmpty()) {
                Button(onClick = ::doShare, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Icon(Icons.Rounded.Share, null, modifier = Modifier.padding(end = 8.dp))
                    Text("Share ${sel.size} line(s)")
                }
            }
        }
    }
}
