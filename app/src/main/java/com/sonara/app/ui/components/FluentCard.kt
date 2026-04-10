package com.sonara.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sonara.app.ui.theme.SonaraCard
import com.sonara.app.ui.theme.SonaraDivider
import com.sonara.app.ui.theme.SonaraSurfaceContainerHigh

/**
 * MD3 Expressive FluentCard
 *
 * Değişiklikler:
 *  - Daha büyük border-radius (MaterialTheme.shapes.large → 26 dp)
 *  - İnce border yerine daha belirgin kapsayıcı rengi
 *  - İsteğe bağlı tıklama + press ölçek animasyonu (expressive motion)
 *  - Üç varyant: normal / elevated / tonal
 */
@Composable
fun FluentCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    onClick: (() -> Unit)? = null,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Expressive press feedback — hafif küçülme
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.97f else 1f,
        label = "cardPressScale"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .then(
                if (onClick != null)
                    Modifier.clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
                else Modifier
            ),
        shape      = shape,
        color      = SonaraCard,
        shadowElevation = if (isPressed) 0.dp else 1.dp,
        border     = BorderStroke(0.5.dp, SonaraDivider.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(padding), content = content)
    }
}

/** Daha belirgin arka plan, elevation olmadan */
@Composable
fun FluentTonalCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    onClick: (() -> Unit)? = null,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.97f else 1f,
        label = "tonalCardPressScale"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .then(
                if (onClick != null)
                    Modifier.clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
                else Modifier
            ),
        shape  = shape,
        color  = SonaraSurfaceContainerHigh,
    ) {
        Column(modifier = Modifier.padding(padding), content = content)
    }
}
