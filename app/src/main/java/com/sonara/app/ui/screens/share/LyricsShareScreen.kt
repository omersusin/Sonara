package com.sonara.app.ui.screens.share

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sonara.app.intelligence.lyrics.LyricLine
import com.sonara.app.ui.theme.SonaraTextPrimary
import com.sonara.app.ui.theme.SonaraTextSecondary
import com.sonara.app.ui.theme.SonaraTextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsShareScreen(
    title: String,
    artist: String,
    lines: List<LyricLine>,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val p = MaterialTheme.colorScheme.primary
    val selectedIndices = remember { mutableStateListOf<Int>() }

    fun doShare() {
        if (selectedIndices.isEmpty()) return
        val text = selectedIndices.sorted()
            .mapNotNull { lines.getOrNull(it)?.text }
            .joinToString("\n")
        val body = buildString {
            appendLine(text); appendLine()
            append("— $title")
            if (artist.isNotBlank()) append(" by $artist")
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, body)
        }
        context.startActivity(Intent.createChooser(intent, "Share lyrics"))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Share Lyrics") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = ::doShare) {
                        Icon(Icons.Rounded.Share, "Share", tint = if (selectedIndices.isEmpty()) SonaraTextTertiary else p)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = SonaraTextPrimary)
                if (artist.isNotBlank()) Text(artist, style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (selectedIndices.isEmpty()) "Tap lines to select" else "${selectedIndices.size} line(s) selected",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selectedIndices.isEmpty()) SonaraTextTertiary else p
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(lines, key = { idx, _ -> idx }) { idx, line ->
                    val selected = idx in selectedIndices
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (selected) p.copy(alpha = 0.15f) else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { if (selected) selectedIndices.remove(idx) else selectedIndices.add(idx) }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = line.text,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (selected) p else SonaraTextPrimary
                        )
                    }
                }
            }

            if (selectedIndices.isNotEmpty()) {
                Button(
                    onClick = ::doShare,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Icon(Icons.Rounded.Share, null, modifier = Modifier.padding(end = 8.dp))
                    Text("Share ${selectedIndices.size} line(s)")
                }
            }
        }
    }
}
