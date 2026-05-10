package com.sonara.app.ui.components.tab

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.sonara.app.ui.common.LocalDynamicColor
import com.sonara.app.ui.common.LocalSeedColor
import com.sonara.app.ui.common.LocalTonalPalette
import com.sonara.app.ui.data.provider.SeedColor
import com.sonara.app.ui.domain.provider.SeedColorProvider
import com.sonara.app.ui.screens.settings.SettingsViewModel
import `in`.hridayan.shapeindicators.ShapeIndicatorRow

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ColorTabs(
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel
) {
    val tonalPalettes = LocalTonalPalette.current
    val groupedPalettes = tonalPalettes.chunked(4)
    val pagerState = rememberPagerState(initialPage = 0) { groupedPalettes.size }

    Column(modifier = modifier) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                groupedPalettes[page].forEach { palette ->
                    val isChecked = LocalSeedColor.current == palette.colors
                    val isDynamicColor = LocalDynamicColor.current

                    PaletteWheel(
                        modifier = Modifier.size(70.dp),
                        seedColor = palette.colors,
                        onClick = {
                            settingsViewModel.setSeedColor(palette.colors)
                            settingsViewModel.setDynamicColorEnabled(false)
                        },
                        isChecked = isChecked && !isDynamicColor,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        ShapeIndicatorRow(
            modifier = Modifier
                .width(120.dp)
                .align(Alignment.CenterHorizontally),
            pagerState = pagerState,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            shuffleShapes = true
        )
    }
}

@Composable
private fun PaletteWheel(
    modifier: Modifier = Modifier,
    seedColor: SeedColor = SeedColorProvider.seed,
    isChecked: Boolean = false,
    onClick: () -> Unit
) {
    val checkedIconScale by animateFloatAsState(
        targetValue = if (isChecked) 1f else 0f,
        animationSpec = if (isChecked) spring(dampingRatio = Spring.DampingRatioMediumBouncy) else tween(
            durationMillis = 300,
            easing = LinearEasing
        ),
        label = "Check Scale Animation"
    )

    val primaryColor = modifyColorForDisplay(Color(seedColor.primary), toneFactor = 1f)
    val secondaryColor = modifyColorForDisplay(Color(seedColor.secondary), toneFactor = 1.4f)
    val tertiaryColor = modifyColorForDisplay(Color(seedColor.tertiary), toneFactor = 0.7f)

    Card(
        modifier = modifier,
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Box(
            modifier = Modifier
                .padding(10.dp)
                .clip(CircleShape)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(color = primaryColor)
                )

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .background(color = secondaryColor)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .background(color = tertiaryColor)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(20.dp)
                    .align(Alignment.Center)
                    .scale(checkedIconScale)
                    .clip(CircleShape)
                    .background(color = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Icon(
                    modifier = Modifier
                        .size(16.dp)
                        .align(Alignment.Center),
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

private fun modifyColorForDisplay(
    color: Color,
    toneFactor: Float = 1.2f,
    chromaFactor: Float = 1.15f
): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)

    hsv[1] = (hsv[1] * chromaFactor).coerceIn(0f, 1f)
    hsv[2] = (hsv[2] * toneFactor).coerceIn(0f, 1f)

    return Color(android.graphics.Color.HSVToColor(hsv))
}