package com.sonara.app.ui.screens.onboarding

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.sonara.app.service.SonaraNotificationListener
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
    val totalPages = pages.size + 2  // info pages + permissions + API keys
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
                page == pages.size -> PermissionsPage(p, audioGranted, audioLauncher, ctx)
                page == pages.size + 1 -> ApiKeysPage(p, ctx)
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

@Composable
private fun PermissionsPage(
    p: androidx.compose.ui.graphics.Color,
    audioGranted: Boolean,
    audioLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    ctx: android.content.Context
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Rounded.Mic, null, Modifier.size(80.dp), tint = p)
        Spacer(Modifier.height(32.dp))
        Text("Permissions", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, color = SonaraTextPrimary)
        Spacer(Modifier.height(16.dp))
        Text("Sonara needs these permissions to work properly.",
            style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = SonaraTextSecondary)
        Spacer(Modifier.height(32.dp))
        OutlinedButton(
            onClick = { audioLauncher.launch(Manifest.permission.RECORD_AUDIO) },
            modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = if (audioGranted) SonaraSuccess else p)
        ) {
            Icon(Icons.Rounded.Mic, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
            Text(if (audioGranted) "Audio Access Granted" else "Allow Audio Access")
        }
        Text("For real-time audio analysis and visualizer", style = MaterialTheme.typography.bodySmall, color = SonaraTextTertiary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        var notifGranted by remember { mutableStateOf(SonaraNotificationListener.isEnabled(ctx)) }
        val lifecycleOwner = LocalLifecycleOwner.current
        LaunchedEffect(lifecycleOwner) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                notifGranted = SonaraNotificationListener.isEnabled(ctx)
            }
        }
        OutlinedButton(
            onClick = { if (!notifGranted) ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
            modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = if (notifGranted) SonaraSuccess else p)
        ) {
            Icon(Icons.Rounded.Notifications, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
            Text(if (notifGranted) "Notification Access Granted" else "Enable Notification Access")
        }
        Text("To detect which media is playing", style = MaterialTheme.typography.bodySmall, color = SonaraTextTertiary, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ApiKeysPage(p: androidx.compose.ui.graphics.Color, ctx: android.content.Context) {
    val app = SonaraApp.instance
    val scope = rememberCoroutineScope()

    // Key states
    var lastfmKey by remember { mutableStateOf("") }
    var lastfmSecret by remember { mutableStateOf("") }
    var geminiKey by remember { mutableStateOf("") }
    var openRouterKey by remember { mutableStateOf("") }
    var groqKey by remember { mutableStateOf("") }
    var githubToken by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf(false) }
    var showGuide by remember { mutableStateOf(false) }

    if (showGuide) {
        AlertDialog(
            onDismissRequest = { showGuide = false },
            containerColor = SonaraCard,
            title = { Text("Where to get API keys") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Last.fm:", style = MaterialTheme.typography.titleSmall, color = SonaraTextPrimary)
                    Text("last.fm/api/account/create\nCreate app, copy API Key + Shared Secret", style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
                    HorizontalDivider(color = SonaraDivider.copy(0.3f))
                    Text("Gemini:", style = MaterialTheme.typography.titleSmall, color = SonaraTextPrimary)
                    Text("aistudio.google.com/apikey\nCreate API key, copy it", style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
                    HorizontalDivider(color = SonaraDivider.copy(0.3f))
                    Text("OpenRouter:", style = MaterialTheme.typography.titleSmall, color = SonaraTextPrimary)
                    Text("openrouter.ai/keys\nCreate key, copy it", style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
                    HorizontalDivider(color = SonaraDivider.copy(0.3f))
                    Text("Groq:", style = MaterialTheme.typography.titleSmall, color = SonaraTextPrimary)
                    Text("console.groq.com/keys\nCreate key, copy it", style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
                    HorizontalDivider(color = SonaraDivider.copy(0.3f))
                    Text("GitHub PAT:", style = MaterialTheme.typography.titleSmall, color = SonaraTextPrimary)
                    Text("github.com/settings/tokens\nFine-grained, repo: sonara-models, contents: read/write", style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
                }
            },
            confirmButton = { TextButton(onClick = { showGuide = false }) { Text("Got it") } }
        )
    }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 32.dp).padding(top = 60.dp, bottom = 120.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Rounded.Key, null, Modifier.size(48.dp), tint = p)
        Spacer(Modifier.height(8.dp))
        Text("API Keys (Optional)", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, color = SonaraTextPrimary)
        Text("You can set these up now or later in Settings. All keys are stored securely on your device.",
            style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = SonaraTextSecondary)
        TextButton(onClick = { showGuide = true }) { Text("Where to get these keys?", color = p) }

        Spacer(Modifier.height(8.dp))

        // Last.fm
        Text("Last.fm", style = MaterialTheme.typography.labelLarge, color = SonaraTextPrimary, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = lastfmKey, onValueChange = { lastfmKey = it }, label = { Text("API Key") },
            modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation())
        OutlinedTextField(value = lastfmSecret, onValueChange = { lastfmSecret = it }, label = { Text("Shared Secret") },
            modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation())

        Spacer(Modifier.height(4.dp))

        // Gemini
        Text("Gemini", style = MaterialTheme.typography.labelLarge, color = SonaraTextPrimary, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = geminiKey, onValueChange = { geminiKey = it }, label = { Text("API Key") },
            modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation())

        Spacer(Modifier.height(4.dp))

        // OpenRouter
        Text("OpenRouter", style = MaterialTheme.typography.labelLarge, color = SonaraTextPrimary, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = openRouterKey, onValueChange = { openRouterKey = it }, label = { Text("API Key") },
            modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation())

        Spacer(Modifier.height(4.dp))

        // Groq
        Text("Groq", style = MaterialTheme.typography.labelLarge, color = SonaraTextPrimary, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = groqKey, onValueChange = { groqKey = it }, label = { Text("API Key") },
            modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation())

        Spacer(Modifier.height(4.dp))

        // GitHub PAT
        Text("GitHub (Community)", style = MaterialTheme.typography.labelLarge, color = SonaraTextPrimary, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = githubToken, onValueChange = { githubToken = it }, label = { Text("Personal Access Token") },
            modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation())

        Spacer(Modifier.height(12.dp))

        // Save all
        OutlinedButton(
            onClick = {
                scope.launch {
                    if (lastfmKey.isNotBlank()) { app.secureSecrets.setLastFmApiKey(lastfmKey); app.preferences.setLastFmApiKey(lastfmKey) }
                    if (lastfmSecret.isNotBlank()) { app.secureSecrets.setLastFmSharedSecret(lastfmSecret); app.preferences.setLastFmSharedSecret(lastfmSecret) }
                    if (geminiKey.isNotBlank()) { app.preferences.setGeminiApiKey(geminiKey) }
                    if (openRouterKey.isNotBlank()) { app.preferences.setOpenRouterApiKey(openRouterKey); app.insightManager.configureOpenRouter(openRouterKey, "google/gemini-2.5-flash") }
                    if (groqKey.isNotBlank()) { app.preferences.setGroqApiKey(groqKey); app.insightManager.configureGroq(groqKey, "llama-3.3-70b-versatile") }
                    if (githubToken.isNotBlank()) { app.secureSecrets.setGitHubToken(githubToken) }
                    if (lastfmKey.isNotBlank()) app.reloadPipeline()
                    saved = true
                    Toast.makeText(ctx, "Keys saved!", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge,
            enabled = lastfmKey.isNotBlank() || geminiKey.isNotBlank() || openRouterKey.isNotBlank() || groqKey.isNotBlank() || githubToken.isNotBlank(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = if (saved) SonaraSuccess else p)
        ) { Text(if (saved) "All Keys Saved!" else "Save All Keys") }
    }
}
