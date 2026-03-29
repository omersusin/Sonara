package com.sonara.app.ui.screens.onboarding

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sonara.app.ui.theme.*
import kotlinx.coroutines.launch

data class OnboardingPage(val icon: ImageVector, val title: String, val description: String)

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun OnboardingScreen(onComplete: () -> Unit) {
    val pages = listOf(
        OnboardingPage(Icons.Rounded.MusicNote, "Sonara listens to your music",
            "Real-time audio analysis detects the character of every song and automatically adjusts your equalizer."),
        OnboardingPage(Icons.Rounded.GraphicEq, "Gets smarter over time",
            "Give feedback and Sonara learns your preferences. Connect Last.fm for even richer analysis."),
    )
    // Total: info pages + permissions page
    val totalPages = pages.size + 1
    val pagerState = rememberPagerState(pageCount = { totalPages })
    val scope = rememberCoroutineScope()
    val p = MaterialTheme.colorScheme.primary
    val ctx = LocalContext.current

    // Permission states
    var audioGranted by remember { mutableStateOf(false) }
    val audioLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        audioGranted = granted
    }

    Box(Modifier.fillMaxSize().background(SonaraBackground)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            when {
                page < pages.size -> {
                    // Info pages
                    Column(Modifier.fillMaxSize().padding(horizontal = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(pages[page].icon, null, Modifier.size(80.dp), tint = p)
                        Spacer(Modifier.height(32.dp))
                        Text(pages[page].title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, color = SonaraTextPrimary)
                        Spacer(Modifier.height(16.dp))
                        Text(pages[page].description, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = SonaraTextSecondary)
                    }
                }
                page == pages.size -> {
                    // Permissions page
                    Column(Modifier.fillMaxSize().padding(horizontal = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Rounded.Mic, null, Modifier.size(80.dp), tint = p)
                        Spacer(Modifier.height(32.dp))
                        Text("Permissions", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, color = SonaraTextPrimary)
                        Spacer(Modifier.height(16.dp))
                        Text("Sonara needs a few permissions to work properly.",
                            style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = SonaraTextSecondary)
                        Spacer(Modifier.height(32.dp))

                        // Audio permission
                        OutlinedButton(
                            onClick = { audioLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.extraLarge,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = if (audioGranted) SonaraSuccess else p)
                        ) {
                            Icon(Icons.Rounded.Mic, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (audioGranted) "Audio Access Granted" else "Allow Audio Access")
                        }
                        Text("Required for real-time audio analysis and visualizer",
                            style = MaterialTheme.typography.bodySmall, color = SonaraTextTertiary, textAlign = TextAlign.Center)

                        Spacer(Modifier.height(16.dp))

                        // Notification listener
                        OutlinedButton(
                            onClick = { ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.extraLarge,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = p)
                        ) {
                            Icon(Icons.Rounded.Notifications, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Enable Notification Access")
                        }
                        Text("Required to detect which song is playing",
                            style = MaterialTheme.typography.bodySmall, color = SonaraTextTertiary, textAlign = TextAlign.Center)
                    }
                }
            }
        }
        Column(Modifier.align(Alignment.BottomCenter).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(totalPages) { i ->
                    Box(Modifier.size(if (i == pagerState.currentPage) 10.dp else 8.dp).clip(CircleShape)
                        .background(if (i == pagerState.currentPage) p else SonaraCardElevated))
                }
            }
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onComplete) { Text("Skip", color = SonaraTextTertiary) }
                if (pagerState.currentPage < totalPages - 1) {
                    FilledTonalButton(onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = p, contentColor = SonaraBackground)) { Text("Next") }
                } else {
                    FilledTonalButton(onClick = onComplete,
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = p, contentColor = SonaraBackground)) { Text("Get Started") }
                }
            }
        }
    }
}
