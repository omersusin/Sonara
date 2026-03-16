package com.sonara.app.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sonara.app.ui.theme.SonaraBackground
import com.sonara.app.ui.theme.SonaraCardElevated
import com.sonara.app.ui.theme.SonaraTextPrimary
import com.sonara.app.ui.theme.SonaraTextSecondary
import com.sonara.app.ui.theme.SonaraTextTertiary
import kotlinx.coroutines.launch

data class OnboardingPage(val icon: ImageVector, val title: String, val description: String)

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val pages = listOf(
        OnboardingPage(Icons.Rounded.MusicNote, "Sonara understands your music", "AI-powered genre detection analyzes every song and automatically adjusts your equalizer for the best sound."),
        OnboardingPage(Icons.Rounded.Headphones, "Knows your headphones", "AutoEQ correction profiles for 26+ headphone models. Connect your headphones and Sonara adapts instantly."),
        OnboardingPage(Icons.Rounded.GraphicEq, "Smarter with Last.fm", "Connect Last.fm for accurate genre detection. Without it, Sonara's local AI handles everything offline.")
    )
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val p = MaterialTheme.colorScheme.primary

    Box(Modifier.fillMaxSize().background(SonaraBackground)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            Column(
                Modifier.fillMaxSize().padding(horizontal = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(pages[page].icon, null, Modifier.size(80.dp), tint = p)
                Spacer(Modifier.height(32.dp))
                Text(pages[page].title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, color = SonaraTextPrimary)
                Spacer(Modifier.height(16.dp))
                Text(pages[page].description, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = SonaraTextSecondary)
            }
        }

        Column(Modifier.align(Alignment.BottomCenter).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(pages.size) { i ->
                    Box(Modifier.size(if (i == pagerState.currentPage) 10.dp else 8.dp).clip(CircleShape)
                        .background(if (i == pagerState.currentPage) p else SonaraCardElevated))
                }
            }
            Spacer(Modifier.height(24.dp))

            if (pagerState.currentPage == pages.size - 1) {
                FilledTonalButton(onClick = onComplete, Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = p, contentColor = SonaraBackground)
                ) { Text("Get Started", style = MaterialTheme.typography.labelLarge) }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = onComplete) { Text("Skip", color = SonaraTextTertiary) }
                    FilledTonalButton(onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = p, contentColor = SonaraBackground)
                    ) { Text("Next") }
                }
            }
        }
    }
}
