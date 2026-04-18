package com.sonara.app.ui.screens.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.ComponentActivity
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sonara.app.ui.theme.SonaraCardElevated
import com.sonara.app.ui.theme.SonaraTextPrimary
import com.sonara.app.ui.theme.SonaraTextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllGenresScreen(onBack: () -> Unit) {
    val activity = LocalContext.current as ComponentActivity
    val vm: InsightsViewModel = viewModel(viewModelStoreOwner = activity)
    val s by vm.uiState.collectAsState()
    val p = MaterialTheme.colorScheme.primary

    val sorted = s.genreDistribution.entries.sortedByDescending { it.value }
    val total = sorted.sumOf { it.value }.toFloat().coerceAtLeast(1f)
    val maxVal = sorted.firstOrNull()?.value?.toFloat() ?: 1f

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Genres (${sorted.size})") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(sorted) { i, (genre, count) ->
                val pct = (count / total * 100).toInt()
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("${i + 1}", style = MaterialTheme.typography.labelLarge, color = if (i < 3) p else SonaraTextTertiary, modifier = Modifier.width(26.dp))
                    Text(genre, style = MaterialTheme.typography.bodyMedium, color = SonaraTextPrimary, modifier = Modifier.width(130.dp), maxLines = 1)
                    Box(Modifier.weight(1f).height(28.dp).clip(RoundedCornerShape(6.dp)).background(SonaraCardElevated)) {
                        Box(Modifier.fillMaxWidth(count / maxVal).height(28.dp).clip(RoundedCornerShape(6.dp)).background(p.copy(alpha = 0.65f)))
                        Text("$pct%", style = MaterialTheme.typography.labelSmall, color = SonaraTextPrimary, modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp))
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
