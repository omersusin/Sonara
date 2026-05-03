package com.sonara.app.ui.screens.insights

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.LinearGradient
import android.graphics.Paint as AndroidPaint
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollageScreen(
    albums: List<Triple<String, String, String>>,
    onBack: () -> Unit = {}
) {
    var gridSize by remember { mutableIntStateOf(3) }
    var showCaptions by remember { mutableStateOf(true) }
    var showBorders by remember { mutableStateOf(false) }
    var showUsername by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val vm: InsightsViewModel = viewModel()
    val s by vm.uiState.collectAsState()

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Album Collage") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back") } },
            actions = {
                IconButton(
                    onClick = {
                        isExporting = true
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                try {
                                    val cellPx = 300
                                    val footerPx = if (showUsername) 60 else 0
                                    val bitmapW = gridSize * cellPx
                                    val bitmapH = gridSize * cellPx + footerPx
                                    val bitmap = Bitmap.createBitmap(bitmapW, bitmapH, Bitmap.Config.ARGB_8888)
                                    val canvas = AndroidCanvas(bitmap)
                                    canvas.drawColor(android.graphics.Color.BLACK)

                                    val displayedAlbums = albums.take(gridSize * gridSize)
                                    val loader = ImageLoader(ctx)
                                    displayedAlbums.forEachIndexed { i, (name, artist, url) ->
                                        val col = i % gridSize
                                        val row = i / gridSize
                                        val left = col * cellPx
                                        val top = row * cellPx

                                        if (url.isNotBlank()) {
                                            try {
                                                val req = ImageRequest.Builder(ctx).data(url).size(cellPx).build()
                                                val result = loader.execute(req)
                                                result.drawable?.let { drawable ->
                                                    drawable.setBounds(left, top, left + cellPx, top + cellPx)
                                                    drawable.draw(canvas)
                                                }
                                            } catch (_: Exception) {}
                                        } else {
                                            val fillPaint = AndroidPaint().apply { color = android.graphics.Color.DKGRAY }
                                            canvas.drawRect(left.toFloat(), top.toFloat(), (left + cellPx).toFloat(), (top + cellPx).toFloat(), fillPaint)
                                            val textPaint = AndroidPaint().apply {
                                                color = android.graphics.Color.WHITE
                                                textSize = cellPx * 0.25f
                                                textAlign = android.graphics.Paint.Align.CENTER
                                                isAntiAlias = true
                                            }
                                            canvas.drawText(name.take(2).uppercase(), (left + cellPx / 2).toFloat(), (top + cellPx / 2 + cellPx * 0.08f), textPaint)
                                        }

                                        // Borders
                                        if (showBorders) {
                                            val borderPaint = AndroidPaint().apply {
                                                color = android.graphics.Color.BLACK
                                                strokeWidth = 3f
                                                style = AndroidPaint.Style.STROKE
                                            }
                                            canvas.drawRect(left.toFloat(), top.toFloat(), (left + cellPx).toFloat(), (top + cellPx).toFloat(), borderPaint)
                                        }

                                        // Captions with gradient
                                        if (showCaptions) {
                                            val gradPaint = AndroidPaint()
                                            gradPaint.shader = LinearGradient(
                                                left.toFloat(), (top + cellPx * 0.6f),
                                                left.toFloat(), (top + cellPx).toFloat(),
                                                android.graphics.Color.TRANSPARENT,
                                                android.graphics.Color.argb(200, 0, 0, 0),
                                                Shader.TileMode.CLAMP
                                            )
                                            canvas.drawRect(left.toFloat(), (top + cellPx * 0.6f), (left + cellPx).toFloat(), (top + cellPx).toFloat(), gradPaint)

                                            val captionPaint = AndroidPaint().apply {
                                                color = android.graphics.Color.WHITE
                                                textSize = cellPx * 0.09f
                                                isAntiAlias = true
                                                typeface = Typeface.DEFAULT_BOLD
                                            }
                                            val captionX = left + cellPx * 0.05f
                                            canvas.drawText(name.take(20), captionX, (top + cellPx * 0.88f), captionPaint)
                                            captionPaint.textSize = cellPx * 0.075f
                                            captionPaint.typeface = Typeface.DEFAULT
                                            captionPaint.color = android.graphics.Color.argb(200, 255, 255, 255)
                                            canvas.drawText(artist.take(20), captionX, (top + cellPx * 0.94f), captionPaint)
                                        }
                                    }

                                    // Username footer
                                    if (showUsername && s.lastFmUsername.isNotBlank()) {
                                        val footerY = (gridSize * cellPx).toFloat()
                                        val footerPaint = AndroidPaint().apply { color = android.graphics.Color.BLACK }
                                        canvas.drawRect(0f, footerY, bitmapW.toFloat(), bitmapH.toFloat(), footerPaint)
                                        val userPaint = AndroidPaint().apply {
                                            color = android.graphics.Color.WHITE
                                            textSize = footerPx * 0.45f
                                            isAntiAlias = true
                                            textAlign = android.graphics.Paint.Align.CENTER
                                        }
                                        canvas.drawText("${s.lastFmUsername} · Sonara", bitmapW / 2f, footerY + footerPx * 0.65f, userPaint)
                                    }

                                    val file = File(ctx.cacheDir, "sonara_collage.png")
                                    FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 95, it) }

                                    val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)

                                    withContext(Dispatchers.Main) {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "image/png"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        ctx.startActivity(Intent.createChooser(shareIntent, "Share Collage"))
                                    }
                                } catch (_: Exception) {
                                } finally {
                                    isExporting = false
                                }
                            }
                        }
                    },
                    enabled = !isExporting && albums.isNotEmpty()
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Rounded.Share, contentDescription = "Share")
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Grid size chips
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(3 to "3×3", 4 to "4×4", 5 to "5×5").forEach { (size, label) ->
                    FilterChip(selected = gridSize == size, onClick = { gridSize = size }, label = { Text(label) })
                }
            }
            // Toggle option chips
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = showCaptions, onClick = { showCaptions = !showCaptions }, label = { Text("Captions") })
                FilterChip(selected = showBorders, onClick = { showBorders = !showBorders }, label = { Text("Borders") })
                FilterChip(selected = showUsername, onClick = { showUsername = !showUsername }, label = { Text("Username") })
            }
            val count = gridSize * gridSize
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridSize),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(if (showBorders) 2.dp else 1.dp),
                verticalArrangement = Arrangement.spacedBy(if (showBorders) 2.dp else 1.dp)
            ) {
                items(albums.take(count)) { (name, _, url) ->
                    Box(Modifier.aspectRatio(1f).clip(RoundedCornerShape(if (showBorders) 0.dp else 4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)) {
                        if (url.isNotBlank()) {
                            AsyncImage(model = url, contentDescription = name,
                                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(name.take(2).uppercase(), style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
