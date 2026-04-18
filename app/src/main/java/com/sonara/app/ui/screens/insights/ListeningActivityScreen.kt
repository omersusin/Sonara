package com.sonara.app.ui.screens.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sonara.app.ui.components.FluentCard
import com.sonara.app.ui.theme.SonaraCardElevated
import com.sonara.app.ui.theme.SonaraDivider
import com.sonara.app.ui.theme.SonaraTextPrimary
import com.sonara.app.ui.theme.SonaraTextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListeningActivityScreen(onBack: () -> Unit) {
    val vm: InsightsViewModel = viewModel()
    val s by vm.uiState.collectAsState()
    val p = MaterialTheme.colorScheme.primary

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Listening Activity") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Streak + Peak Hour summary
            if (s.streakDays > 0 || s.peakHour >= 0) {
                item {
                    FluentCard {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            if (s.streakDays > 0) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("${s.streakDays}d", style = MaterialTheme.typography.headlineMedium, color = p, fontWeight = FontWeight.Bold)
                                    Text("current streak", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                                }
                            }
                            if (s.peakHour >= 0) {
                                val h = s.peakHour
                                val label = "${if (h % 12 == 0) 12 else h % 12}:00 ${if (h < 12) "AM" else "PM"}"
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(label, style = MaterialTheme.typography.headlineMedium, color = p, fontWeight = FontWeight.Bold)
                                    Text("peak hour", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                                }
                            }
                        }
                    }
                }
            }

            // Day-of-week bar chart
            if (s.weeklyActivity.isNotEmpty() && s.weeklyActivity.any { it.second > 0 }) {
                item {
                    FluentCard {
                        Text("By Day of Week", style = MaterialTheme.typography.titleMedium, color = SonaraTextPrimary)
                        Spacer(Modifier.height(12.dp))
                        val maxCount = s.weeklyActivity.maxOfOrNull { it.second }?.toFloat()?.coerceAtLeast(1f) ?: 1f
                        val total = s.weeklyActivity.sumOf { it.second }
                        Row(Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                            s.weeklyActivity.forEach { (day, count) ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom, modifier = Modifier.weight(1f)) {
                                    Text(count.toString(), style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                                    Spacer(Modifier.height(2.dp))
                                    val barH = if (maxCount > 0) (count / maxCount * 80).dp else 4.dp
                                    Box(Modifier.width(28.dp).height(barH.coerceAtLeast(4.dp)).clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)).background(p.copy(alpha = 0.75f)))
                                    Spacer(Modifier.height(4.dp))
                                    Text(day, style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Box(Modifier.fillMaxWidth().height(0.5.dp).background(SonaraDivider.copy(0.15f)))
                        Spacer(Modifier.height(8.dp))
                        Text("Based on $total recent scrobbles", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                    }
                }
            }

            // 24-hour heatmap legend + by-hour distribution
            if (s.recentTracks.isNotEmpty()) {
                item {
                    FluentCard {
                        Text("By Hour of Day", style = MaterialTheme.typography.titleMedium, color = SonaraTextPrimary)
                        Spacer(Modifier.height(12.dp))
                        val hourBuckets = remember(s.recentTracks) {
                            IntArray(24).also { arr ->
                                s.recentTracks.filter { !it.isNowPlaying && it.uts > 0 }.forEach { t ->
                                    val cal = java.util.Calendar.getInstance().apply { timeInMillis = t.uts * 1000 }
                                    arr[cal.get(java.util.Calendar.HOUR_OF_DAY)]++
                                }
                            }
                        }
                        val maxH = hourBuckets.max().toFloat().coerceAtLeast(1f)
                        // Show 24 bars in two rows of 12
                        listOf(0..11, 12..23).forEach { range ->
                            Row(Modifier.fillMaxWidth().height(60.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                                range.forEach { hour ->
                                    val count = hourBuckets[hour]
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom, modifier = Modifier.weight(1f)) {
                                        val barH = if (maxH > 0) (count / maxH * 40).dp else 2.dp
                                        Box(Modifier.width(14.dp).height(barH.coerceAtLeast(2.dp)).clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)).background(p.copy(alpha = if (count == 0) 0.1f else 0.7f)))
                                        Spacer(Modifier.height(2.dp))
                                        Text("${hour}h", style = MaterialTheme.typography.labelSmall.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.75f), color = SonaraTextTertiary)
                                    }
                                }
                            }
                            if (range.first == 0) Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }

            // Full heatmap
            if (s.heatmap.isNotEmpty()) {
                item { ListeningHeatmapCard(s.heatmap, p) }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun ListeningHeatmapCard(heatmap: Map<String, Int>, p: androidx.compose.ui.graphics.Color) {
    val weeksCount = 16
    val maxCount = remember(heatmap) { heatmap.values.maxOrNull() ?: 1 }
    val sdf = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US) }
    val grid = remember(heatmap) {
        val start = java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.DAY_OF_YEAR, -(weeksCount * 7 - 1))
            set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.SUNDAY)
            set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
        }
        Array(7) { dayOfWeek ->
            IntArray(weeksCount) { week ->
                val c = start.clone() as java.util.Calendar
                c.add(java.util.Calendar.DAY_OF_YEAR, week * 7 + dayOfWeek)
                heatmap[sdf.format(c.time)] ?: 0
            }
        }
    }
    FluentCard {
        Text("Listening Heatmap (16 weeks)", style = MaterialTheme.typography.titleMedium, color = SonaraTextPrimary)
        Spacer(Modifier.height(8.dp))
        val dayLabels = listOf("S", "M", "T", "W", "T", "F", "S")
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            (0..6).forEach { dayOfWeek ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(dayLabels[dayOfWeek], style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary, modifier = Modifier.width(12.dp))
                    (0 until weeksCount).forEach { week ->
                        val count = grid[dayOfWeek][week]
                        val alpha = if (count == 0) 0.07f else (count.toFloat() / maxCount).coerceIn(0.2f, 1f)
                        Box(Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(2.dp)).background(p.copy(alpha = alpha)))
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            Text("Less", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
            Spacer(Modifier.width(4.dp))
            listOf(0.07f, 0.3f, 0.55f, 0.78f, 1f).forEach { alpha ->
                Box(Modifier.padding(horizontal = 1.dp).size(10.dp).clip(RoundedCornerShape(2.dp)).background(p.copy(alpha = alpha)))
            }
            Spacer(Modifier.width(4.dp))
            Text("More", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
        }
    }
}
