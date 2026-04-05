package com.sonara.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.sonara.app.ui.theme.M3ECircle
import com.sonara.app.ui.theme.M3ERoundedSquare
import com.sonara.app.ui.theme.MorphPolygonShape
import com.sonara.app.ui.theme.SonaraCard
import com.sonara.app.ui.theme.SonaraDivider
import com.sonara.app.ui.theme.m3eMorph
import com.sonara.app.ui.theme.rememberPressedMorphAnimation

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FluentCard(
    modifier: Modifier = Modifier,
    morphShapes: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val morph = remember { m3eMorph(M3ERoundedSquare, M3ECircle) }
    val morphProgress = rememberPressedMorphAnimation(
        interactionSource = interactionSource,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = Spring.StiffnessMedium
        )
    )

    val scale by animateFloatAsState(
        targetValue = if (morphProgress > 0.01f) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "m3e_card_press"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                if (morphShapes) {
                    clip = true
                    shape = MorphPolygonShape(morph, morphProgress)
                    shadowElevation = 4f * morphProgress
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        interactionSource.tryEmitPress(offset)
                        try { awaitRelease() } finally {
                            interactionSource.tryEmitRelease()
                        }
                    }
                )
            },
        shape = if (morphShapes) MaterialTheme.shapes.medium else MaterialTheme.shapes.medium,
        color = SonaraCard,
        border = BorderStroke(
            0.6.dp,
            SonaraDivider.copy(alpha = 0.5f + 0.3f * morphProgress)
        ),
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}
