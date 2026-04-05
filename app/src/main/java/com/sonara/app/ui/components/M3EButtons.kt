@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sonara.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.sonara.app.ui.theme.M3ECircle
import com.sonara.app.ui.theme.M3ERoundedSquare
import com.sonara.app.ui.theme.MorphPolygonShape
import com.sonara.app.ui.theme.m3eMorph
import com.sonara.app.ui.theme.rememberPressedMorphAnimation

/**
 * M3E Expressive Filled Button with shape morphing on press.
 * Morphs from rounded-square → circle on interaction per M3E guidelines.
 */
@Composable
fun M3EFilledButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val morph = remember { m3eMorph(M3ERoundedSquare, M3ECircle) }
    val morphProgress = rememberPressedMorphAnimation(
        interactionSource = interactionSource,
        animationSpec = spring(
            dampingRatio = 0.5f,
            stiffness = Spring.StiffnessMedium
        )
    )

    val scale by animateFloatAsState(
        targetValue = if (morphProgress > 0.01f) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
        label = "m3e_button_scale"
    )

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 64.dp, minHeight = 40.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                clip = true
                shape = MorphPolygonShape(morph, morphProgress)
                shadowElevation = 2f + 4f * morphProgress
            }
            .background(
                color = if (enabled) containerColor else containerColor.copy(alpha = 0.12f),
                shape = MorphPolygonShape(morph, morphProgress)
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        ProvideTextStyle(
            MaterialTheme.typography.labelLarge.copy(color = contentColor)
        ) {
            content()
        }
    }
}

/**
 * M3E Expressive Outlined Button with shape morphing on press.
 */
@Composable
fun M3EOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    contentColor: Color = MaterialTheme.colorScheme.primary,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val morph = remember { m3eMorph(M3ERoundedSquare, M3ECircle) }
    val morphProgress = rememberPressedMorphAnimation(
        interactionSource = interactionSource,
        animationSpec = spring(
            dampingRatio = 0.5f,
            stiffness = Spring.StiffnessMedium
        )
    )

    val scale by animateFloatAsState(
        targetValue = if (morphProgress > 0.01f) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
        label = "m3e_outlined_button_scale"
    )

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 64.dp, minHeight = 40.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                clip = true
                shape = MorphPolygonShape(morph, morphProgress)
            }
            .background(
                color = Color.Transparent,
                shape = MorphPolygonShape(morph, morphProgress)
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        // Border via nested Box
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    clip = true
                    shape = MorphPolygonShape(morph, morphProgress)
                }
                .background(
                    Color.Transparent
                ),
        )

        ProvideTextStyle(
            MaterialTheme.typography.labelLarge.copy(color = contentColor)
        ) {
            content()
        }
    }
}
