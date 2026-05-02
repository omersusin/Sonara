package com.sonara.app.ui.screens.insights

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
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
    var isExporting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current

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
                                    val cellPx = 200
                                    val bitmapSize = gridSize * cellPx
                                    val bitmap = Bitmap.createBitmap(bitmapSize, bitmapSize, Bitmap.Config.ARGB_8888)
                                    val canvas = AndroidCanvas(bitmap)
                                    canvas.drawColor(android.graphics.Color.BLACK)

                                    val displayedAlbums = albums.take(gridSize * gridSize)
                                    val loader = ImageLoader(ctx)
                                    displayedAlbums.forEachIndexed { i, (name, _, url) ->
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
                                            val paint = android.graphics.Paint().apply {
                                                color = android.graphics.Color.DKGRAY
                                                textSize = cellPx * 0.3f
                                                textAlign = android.graphics.Paint.Align.CENTER
                                                isAntiAlias = true
                                            }
                                            canvas.drawRect(left.toFloat(), top.toFloat(), (left + cellPx).toFloat(), (top + cellPx).toFloat(), android.graphics.Paint().apply { color = android.graphics.Color.DKGRAY })
                                            canvas.drawText(name.take(2).uppercase(), (left + cellPx / 2).toFloat(), (top + cellPx / 2 + cellPx * 0.1f), paint)
                                        }
                                    }

                                    val file = File(ctx.cacheDir, "sonara_collage.png")
                                    FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 95, it) }

                                    val uri = FileProvider.getUriForFile(
                                        ctx, "${ctx.packageName}.fileprovider", file
                                    )

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
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(3 to "3×3", 4 to "4×4", 5 to "5×5").forEach { (size, label) ->
                    FilterChip(selected = gridSize == size, onClick = { gridSize = size },
                        label = { Text(label) })
                }
            }
            val count = gridSize * gridSize
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridSize),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(albums.take(count)) { (name, _, url) ->
                    Box(Modifier.aspectRatio(1f).clip(RoundedCornerShape(4.dp))
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
