package com.sonara.app.ui.screens.debug

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonara.app.data.LogLevel
import com.sonara.app.data.SonaraLogger
import com.sonara.app.ui.theme.*

@Composable
fun DebugLogScreen(onBack: () -> Unit) {
    val logs by SonaraLogger.logs.collectAsState()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val p = MaterialTheme.colorScheme.primary

    var selectedFilter by remember { mutableStateOf<LogLevel?>(null) }
    val filteredLogs = if (selectedFilter != null) logs.filter { it.level == selectedFilter } else logs

    // Auto-scroll to bottom
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) listState.animateScrollToItem(filteredLogs.size - 1)
    }

    Column(Modifier.fillMaxSize()) {
        // Header
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back", tint = SonaraTextPrimary) }
                Column {
                    Text("Debug Log", style = MaterialTheme.typography.titleLarge)
                    Text("${logs.size} entries", style = MaterialTheme.typography.bodySmall, color = SonaraTextTertiary)
                }
            }
            Row {
                IconButton(onClick = {
                    clipboard.setText(AnnotatedString(SonaraLogger.exportAsText()))
                    Toast.makeText(context, "Log copied to clipboard (${logs.size} entries)", Toast.LENGTH_SHORT).show()
                }) { Icon(Icons.Rounded.ContentCopy, "Copy", tint = p) }

                IconButton(onClick = { SonaraLogger.clear() }) {
                    Icon(Icons.Rounded.Delete, "Clear", tint = SonaraError)
                }
            }
        }

        // Filter chips
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedFilter == null,
                onClick = { selectedFilter = null },
                label = { Text("All", style = MaterialTheme.typography.labelSmall) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = p.copy(0.15f), selectedLabelColor = p,
                    containerColor = SonaraCard, labelColor = SonaraTextSecondary
                )
            )
            LogLevel.entries.forEach { level ->
                val count = logs.count { it.level == level }
                if (count > 0) {
                    FilterChip(
                        selected = selectedFilter == level,
                        onClick = { selectedFilter = if (selectedFilter == level) null else level },
                        label = { Text("${level.emoji} ${level.tag} ($count)", style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = p.copy(0.15f), selectedLabelColor = p,
                            containerColor = SonaraCard, labelColor = SonaraTextSecondary
                        )
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Log entries
        LazyColumn(
            Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(filteredLogs.size) { index -> val entry = filteredLogs[index] { entry ->
                val bgColor = when (entry.level) {
                    LogLevel.ERROR -> SonaraError.copy(0.1f)
                    LogLevel.WARN -> SonaraWarning.copy(0.08f)
                    LogLevel.EQ -> MaterialTheme.colorScheme.primary.copy(0.06f)
                    LogLevel.AI -> SonaraInfo.copy(0.06f)
                    else -> SonaraCard.copy(0.3f)
                }
                val textColor = when (entry.level) {
                    LogLevel.ERROR -> SonaraError
                    LogLevel.WARN -> SonaraWarning
                    else -> SonaraTextSecondary
                }

                Text(
                    entry.displayText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bgColor, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    ),
                    color = textColor
                )
            }
        }
    }
}
