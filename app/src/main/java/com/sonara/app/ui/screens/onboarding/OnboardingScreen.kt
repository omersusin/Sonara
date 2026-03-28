package com.sonara.app.ui.screens.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sonara.app.ai.SonaraAi
import com.sonara.app.service.SonaraNotificationListener
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
    val totalPages = pages.size + 1
    val pagerState = rememberPagerState(pageCount = { totalPages })
    val scope = rememberCoroutineScope()
    val p = MaterialTheme.colorScheme.primary
    val np by SonaraNotificationListener.nowPlaying.collectAsState()
    val isPlaying = np != null && np?.title?.isNotBlank() == true

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
                    HearTheDifferenceScreen(isPlaying = isPlaying, onContinue = onComplete,
                        onSetEqEnabled = { enabled -> SonaraAi.getInstance()?.setEqEnabled(enabled) })
                }
            }
        }
        if (pagerState.currentPage < pages.size) {
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
                    FilledTonalButton(onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = p, contentColor = SonaraBackground)) { Text("Next") }
                }
            }
        }
    }
}
