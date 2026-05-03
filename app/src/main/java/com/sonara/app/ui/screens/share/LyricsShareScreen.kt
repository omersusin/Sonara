package com.sonara.app.ui.screens.share

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.LinearGradient
import android.graphics.Paint as AndroidPaint
import android.graphics.Shader
import android.graphics.Typeface
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.sonara.app.intelligence.lyrics.LyricLine
import com.sonara.app.intelligence.odesli.OdesliHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class ShareCardTheme(val label: String) {
    MINIMAL("Minimal"), QUOTE("Quote"), GRADIENT("Gradient"), SPOTIFY("Spotify")
}

private fun buildLyricsShareBitmap(
    selectedTexts: List<String>,
    title: String,
    artist: String,
    theme: ShareCardTheme,
    accentArgb: Int
): Bitmap {
    val w = 1080
    val lineH = 72
    val padding = 80
    val footerH = 100
    val h = padding + selectedTexts.size * lineH + padding + footerH
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = AndroidCanvas(bmp)

    val bgPaint = AndroidPaint().apply { isAntiAlias = true }
    val textPaint = AndroidPaint().apply {
        isAntiAlias = true
        textSize = 52f
        color = android.graphics.Color.WHITE
        typeface = Typeface.DEFAULT_BOLD
    }
    val subPaint = AndroidPaint().apply {
        isAntiAlias = true
        textSize = 36f
        color = android.graphics.Color.argb(160, 255, 255, 255)
        typeface = Typeface.DEFAULT
    }

    when (theme) {
        ShareCardTheme.MINIMAL -> {
            bgPaint.color = android.graphics.Color.BLACK
            c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)
        }
        ShareCardTheme.SPOTIFY -> {
            bgPaint.color = android.graphics.Color.parseColor("#121212")
            c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)
        }
        ShareCardTheme.GRADIENT -> {
            bgPaint.shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(),
                intArrayOf(accentArgb, android.graphics.Color.argb(180, android.graphics.Color.red(accentArgb), android.graphics.Color.green(accentArgb), android.graphics.Color.blue(accentArgb))),
                null, Shader.TileMode.CLAMP)
            c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)
        }
        ShareCardTheme.QUOTE -> {
            bgPaint.color = android.graphics.Color.argb(240, 20, 20, 30)
            c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)
            val quotePaint = AndroidPaint().apply {
                color = android.graphics.Color.argb(80, android.graphics.Color.red(accentArgb), android.graphics.Color.green(accentArgb), android.graphics.Color.blue(accentArgb))
                textSize = 200f
                isAntiAlias = true
                typeface = Typeface.DEFAULT_BOLD
            }
            c.drawText("“", 40f, 180f, quotePaint)
        }
    }

    selectedTexts.forEachIndexed { i, line ->
        val y = padding + i * lineH + lineH * 0.75f
        c.drawText(line, padding.toFloat(), y.toFloat(), textPaint)
    }

    val footerY = padding + selectedTexts.size * lineH + padding.toFloat()
    c.drawText("$title — $artist", padding.toFloat(), footerY + 36f, subPaint)
    subPaint.textSize = 28f
    subPaint.color = android.graphics.Color.argb(100, 255, 255, 255)
    c.drawText("Sonara", padding.toFloat(), footerY + 76f, subPaint)

    return bmp
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
    val scope = rememberCoroutineScope()
    var songLinkLoading by remember { mutableStateOf(false) }
    val accentArgb = accentColor.toArgb()

    fun shareAsImage() {
        if (sel.isEmpty()) return
        val selectedTexts = sel.sorted().mapNotNull { lines.getOrNull(it)?.text }
        scope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val bmp = buildLyricsShareBitmap(selectedTexts, title, artist, theme, accentArgb)
                    val file = File(ctx.cacheDir, "sonara_lyrics.png")
                    FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 95, it) }
                    val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
                    withContext(Dispatchers.Main) {
                        ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                            type = "image/png"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }, "Share lyrics"))
                    }
                } catch (_: Exception) {
                    val text = selectedTexts.joinToString("\n")
                    withContext(Dispatchers.Main) {
                        ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "\"$text\"\n\n$title — $artist\n#Sonara")
                        }, "Share lyrics"))
                    }
                }
            }
        }
    }

    fun shareSongLink() {
        songLinkLoading = true
        scope.launch {
            val url = withContext(Dispatchers.IO) {
                try { OdesliHelper.getSongLinkUrl(title, artist) } catch (_: Exception) { "https://song.link/s/${java.net.URLEncoder.encode("$artist $title", "UTF-8")}" }
            }
            songLinkLoading = false
            ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "\"$title\" by $artist\n$url\n#Sonara")
            }, "Share song link"))
        }
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Share Lyrics") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back") } },
            actions = {
                if (songLinkLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(end = 8.dp), strokeWidth = 2.dp, color = p)
                } else if (sel.isEmpty()) {
                    IconButton(onClick = ::shareSongLink) {
                        Icon(Icons.Rounded.Share, "Share song link", tint = p)
                    }
                } else {
                    IconButton(onClick = ::shareAsImage) {
                        Icon(Icons.Rounded.Share, "Share", tint = p)
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent))
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                if (artist.isNotBlank()) Text(artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(if (sel.isEmpty()) "Tap lines to select · share icon sends song.link" else "${sel.size} line(s) selected · share as image",
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
                Button(onClick = ::shareAsImage, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Icon(Icons.Rounded.Share, null, modifier = Modifier.padding(end = 8.dp))
                    Text("Share ${sel.size} line(s) as image")
                }
            }
        }
    }
}
