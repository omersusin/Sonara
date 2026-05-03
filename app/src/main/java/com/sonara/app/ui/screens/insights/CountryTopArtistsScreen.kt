package com.sonara.app.ui.screens.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sonara.app.SonaraApp
import com.sonara.app.intelligence.deezer.DeezerImageResolver
import com.sonara.app.intelligence.lastfm.GeoArtistItem
import com.sonara.app.intelligence.lastfm.LastFmClient
import com.sonara.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

private val COUNTRIES = listOf(
    "United States", "United Kingdom", "Germany", "France", "Brazil",
    "Japan", "Canada", "Australia", "Sweden", "Netherlands",
    "Spain", "Italy", "Poland", "Russia", "South Korea",
    "Mexico", "Argentina", "Norway", "Finland", "Denmark"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryTopArtistsScreen(
    onBack: () -> Unit,
    onArtistClick: (String) -> Unit = {}
) {
    val app = SonaraApp.instance
    val p = MaterialTheme.colorScheme.primary
    val ctx = LocalContext.current
    val fmt = NumberFormat.getNumberInstance(Locale.getDefault())

    var selectedCountry by remember { mutableStateOf("United States") }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var artists by remember { mutableStateOf<List<GeoArtistItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }

    LaunchedEffect(selectedCountry) {
        loading = true
        val apiKey = app.lastFmAuth.getActiveApiKey()
        if (apiKey.isNotBlank()) {
            withContext(Dispatchers.IO) {
                try {
                    val resp = LastFmClient.api.getGeoTopArtists(selectedCountry, apiKey, 30)
                    artists = resp.topartists?.artist ?: emptyList()
                } catch (_: Exception) {}
            }
        }
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Country Top Artists") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Country picker
            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                OutlinedButton(
                    onClick = { dropdownExpanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(selectedCountry, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Icon(Icons.Rounded.ExpandMore, null, modifier = Modifier.size(20.dp))
                }
                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f).heightIn(max = 300.dp)
                ) {
                    COUNTRIES.forEach { country ->
                        DropdownMenuItem(
                            text = { Text(country, color = if (country == selectedCountry) p else SonaraTextPrimary) },
                            onClick = { selectedCountry = country; dropdownExpanded = false }
                        )
                    }
                }
            }

            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = p) }
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                    itemsIndexed(artists) { i, a ->
                        var imgUrl by remember(a.name) { mutableStateOf(a.imageUrl ?: "") }
                        LaunchedEffect(a.name) {
                            if (imgUrl.isBlank() && !a.name.isNullOrBlank()) {
                                val r = withContext(Dispatchers.IO) { DeezerImageResolver.getArtistImageWithFallback(a.name) ?: "" }
                                if (r.isNotBlank()) imgUrl = r
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth().clickable { onArtistClick(a.name ?: "") }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("${i + 1}", style = MaterialTheme.typography.labelLarge, color = if (i < 3) p else SonaraTextTertiary, modifier = Modifier.width(28.dp))
                            if (imgUrl.isNotBlank()) {
                                AsyncImage(model = ImageRequest.Builder(ctx).data(imgUrl).crossfade(true).build(), contentDescription = null, modifier = Modifier.size(46.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                            } else {
                                Box(Modifier.size(46.dp).background(SonaraCardElevated, CircleShape), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Person, null, tint = p.copy(0.4f), modifier = Modifier.size(18.dp))
                                }
                            }
                            Column(Modifier.weight(1f)) {
                                Text(a.name ?: "", style = MaterialTheme.typography.bodyMedium, color = SonaraTextPrimary, maxLines = 1)
                                if (!a.listeners.isNullOrBlank()) Text(try { "${fmt.format(a.listeners.toLong())} listeners" } catch (_: Exception) { "${a.listeners} listeners" }, style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                            }
                        }
                        if (i < artists.lastIndex) Box(Modifier.fillMaxWidth().padding(start = 86.dp).height(0.5.dp).background(SonaraDivider.copy(0.1f)))
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}
