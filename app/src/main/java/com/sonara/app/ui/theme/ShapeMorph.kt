@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sonara.app.ui.theme

import android.graphics.Matrix
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath

// ═══════════════════════════════════════════════════════
//  M3E Shape Morphing Utilities
//  Based on androidx.graphics:graphics-shapes
// ═══════════════════════════════════════════════════════

/**
 * A [Shape] that wraps a [Morph] and a progress value,
 * producing an interpolated outline for Compose clipping.
 */
class MorphPolygonShape(
    private val morph: Morph,
    private val progress: Float
) : Shape {
    private val androidPath = android.graphics.Path()
    private val matrix = Matrix()

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        androidPath.reset()
        matrix.reset()
        matrix.postTranslate(size.width / 2f, size.height / 2f)
        matrix.postScale(size.width / 2f, size.height / 2f)

        morph.toPath(progress, androidPath)
        val path = Path().apply {
            addPath(androidPath.asComposePath())
        }
        return Outline.Generic(path)
    }
}

/**
 * A [Shape] wrapping a single [RoundedPolygon] for use
 * with `clip()`, `graphicsLayer { shape = … }`, etc.
 */
class RoundedPolygonShape(
    private val polygon: RoundedPolygon
) : Shape {
    private val androidPath = android.graphics.Path()
    private val matrix = Matrix()

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        androidPath.reset()
        matrix.reset()
        matrix.postTranslate(size.width / 2f, size.height / 2f)
        matrix.postScale(size.width / 2f, size.height / 2f)

        polygon.toPath(androidPath)
        val path = Path().apply {
            addPath(androidPath.asComposePath())
        }
        return Outline.Generic(path)
    }
}

// ═══ Predefined M3E RoundedPolygon Shapes ═══
//    Normalized unit-circle polygons for morphing compatibility.

/** Circle — 32-vertex normalized circle */
val M3ECircle = RoundedPolygon(numVertices = 32)

/** Diamond — 4 vertices */
val M3EDiamond = RoundedPolygon(numVertices = 4)

/** Hexagon — 6 vertices */
val M3EHexagon = RoundedPolygon(numVertices = 6)

/** Octagon — 8 vertices */
val M3EOctagon = RoundedPolygon(numVertices = 8)

/** Rounded Square — 4 vertices with corner smoothing */
val M3ERoundedSquare = RoundedPolygon(
    numVertices = 4,
    perVertexRounding = List(4) {
        CornerRounding(radius = 0.35f, smoothing = 0.6f)
    }
)

/** Pill / Stadium — 16 vertices with alternating rounding */
val M3EPill = RoundedPolygon(
    numVertices = 16,
    perVertexRounding = List(16) { i ->
        CornerRounding(
            radius = 0.5f,
            smoothing = if (i % 2 == 0) 1f else 0f
        )
    }
)

/** Star — 10 vertices, alternating inner/outer radius */
val M3EStar: RoundedPolygon by lazy {
    val outerR = 1f
    val innerR = 0.45f
    val verts = FloatArray(20)
    for (i in 0 until 10) {
        val angle = Math.PI * (2 * i - 1) / 10 - Math.PI / 2
        val r = if (i % 2 == 0) outerR else innerR
        verts[i * 2] = (r * Math.cos(angle)).toFloat()
        verts[i * 2 + 1] = (r * Math.sin(angle)).toFloat()
    }
    RoundedPolygon(
        vertices = verts,
        perVertexRounding = List(10) {
            CornerRounding(radius = 0.15f, smoothing = 0.3f)
        }
    )
}

/** Heart — parametric heart curve sampled at 12 points */
val M3EHeart: RoundedPolygon by lazy {
    val verts = FloatArray(24)
    for (i in 0 until 12) {
        val t = 2 * Math.PI * i / 12
        val x = 16 * Math.pow(Math.sin(t), 3.0) / 16
        val y = -(13 * Math.cos(t) - 5 * Math.cos(2 * t) -
                2 * Math.cos(3 * t) - Math.cos(4 * t)) / 16
        verts[i * 2] = x.toFloat()
        verts[i * 2 + 1] = y.toFloat()
    }
    RoundedPolygon(
        vertices = verts,
        perVertexRounding = List(12) {
            CornerRounding(radius = 0.2f, smoothing = 0.5f)
        }
    )
}

// ═══ Morph Helpers ═══

/** Create a [Morph] between two [RoundedPolygon] instances. */
fun m3eMorph(start: RoundedPolygon, end: RoundedPolygon): Morph =
    Morph(start = start, end = end)

/**
 * Composable that animates a float from 0→1 based on press interaction,
 * aligned with M3E MotionScheme default effects spec (~200ms ease-out).
 */
@Composable
fun rememberPressedMorphAnimation(
    interactionSource: MutableInteractionSource,
    animationSpec: AnimationSpec<Float> = tween(
        durationMillis = 200,
        easing = androidx.compose.animation.core.FastOutSlowInEasing
    )
): Float {
    val isPressed by interactionSource.collectIsPressedAsState()
    val progress by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = animationSpec,
        label = "m3ePressedMorph"
    )
    return progress
}

/**
 * Infinite morph animation for decorative / ambient use.
 * Aligned with M3E MotionScheme slow effects spec (~800ms).
 */
@Composable
fun rememberInfiniteMorphProgress(
    durationMillis: Int = 800
): Float {
    val transition = rememberInfiniteTransition(label = "m3eInfiniteMorph")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis),
            repeatMode = RepeatMode.Reverse
        ),
        label = "m3eInfiniteMorphProgress"
    ).value
}

/**
 * Modifier extension that applies a morphed shape clip
 * and optional tonal elevation shadow (M3E tonal depth system).
 */
fun Modifier.morphClip(
    morph: Morph,
    progress: Float,
    tonalElevation: Float = 0f
): Modifier = this
    .graphicsLayer {
        clip = true
        shape = MorphPolygonShape(morph, progress)
        if (tonalElevation > 0f) {
            shadowElevation = tonalElevation
        }
    }
