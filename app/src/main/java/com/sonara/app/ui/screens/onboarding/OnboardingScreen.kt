package com.sonara.app.ui.screens.onboarding

import android.Manifest
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sonara.app.SonaraApp
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
    // info pages + permissions + lastfm setup
    val totalPages = pages.size + 2
    val pagerState = rememberPagerState(pageCount = { totalPages })
    val scope = rememberCoroutineScope()
    val p = MaterialTheme.colorScheme.primary
    val ctx = LocalContext.current

    var audioGranted by remember { mutableStateOf(false) }
    val audioLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { audioGranted = it }

    Box(Modifier.fillMaxSize().background(SonaraBackground)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            when {
                page < pages.size -> {
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
                        OutlinedButton(
                            onClick = { audioLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                            modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = if (audioGranted) SonaraSuccess else p)
                        ) {
                            Icon(Icons.Rounded.Mic, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (audioGranted) "Audio Access Granted" else "Allow Audio Access")
                        }
                        Text("For real-time audio analysis", style = MaterialTheme.typography.bodySmall, color = SonaraTextTertiary, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = { ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                            modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = p)
                        ) {
                            Icon(Icons.Rounded.Notifications, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Enable Notification Access")
                        }
                        Text("To detect which song is playing", style = MaterialTheme.typography.bodySmall, color = SonaraTextTertiary, textAlign = TextAlign.Center)
                    }
                }
                page == pages.size + 1 -> {
                    // Last.fm setup (optional)
                    var apiKey by remember { mutableStateOf("") }
                    var secret by remember { mutableStateOf("") }
                    var saved by remember { mutableStateOf(false) }
                    var showGuide by remember { mutableStateOf(false) }

                    if (showGuide) {
                        AlertDialog(
                            onDismissRequest = { showGuide = false },
                            containerColor = SonaraCard,
                            title = { Text("How to get API keys") },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("1. Go to last.fm/api/account/create", color = SonaraTextPrimary)
                                    Text("2. Application Name: Sonara", color = SonaraTextPrimary)
                                    Text("3. Leave Callback URL empty", color = SonaraTextPrimary)
                                    Text("4. Submit and copy API Key + Shared Secret", color = SonaraTextPrimary)
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    showGuide = false
                                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.last.fm/api/account/create")))
                                }) { Text("Open Last.fm") }
                            },
                            dismissButton = { TextButton(onClick = { showGuide = false }) { Text("Close") } }
                        )
                    }

                    Column(Modifier.fillMaxSize().padding(horizontal = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Rounded.Public, null, Modifier.size(80.dp), tint = p)
                        Spacer(Modifier.height(32.dp))
                        Text("Last.fm (Optional)", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, color = SonaraTextPrimary)
                        Spacer(Modifier.height(16.dp))
                        Text("Connect Last.fm for better genre detection. You can skip this and set it up later in Settings.",
                            style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = SonaraTextSecondary)
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { showGuide = true }) { Text("How to get API keys?", color = p) }
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(value = apiKey, onValueChange = { apiKey = it },
                            label = { Text("API Key") }, modifier = Modifier.fillMaxWidth(),
                            singleLine = true, visualTransformation = PasswordVisualTransformation())
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = secret, onValueChange = { secret = it },
                            label = { Text("Shared Secret") }, modifier = Modifier.fillMaxWidth(),
                            singleLine = true, visualTransformation = PasswordVisualTransformation())
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                if (apiKey.isNotBlank() && secret.isNotBlank()) {
                                    val app = SonaraApp.instance
                                    kotlinx.coroutines.MainScope().launch {
                                        app.secureSecrets.setLastFmApiKey(apiKey)
                                        app.secureSecrets.setLastFmSharedSecret(secret)
                                        app.preferences.setLastFmApiKey(apiKey)
                                        app.preferences.setLastFmSharedSecret(secret)
                                        app.reloadPipeline()
                                        saved = true
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge,
                            enabled = apiKey.isNotBlank() && secret.isNotBlank(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = if (saved) SonaraSuccess else p)
                        ) { Text(if (saved) "Keys Saved!" else "Save Keys") }
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
