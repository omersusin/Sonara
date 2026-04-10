package com.sonara.app.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * MD3 Expressive Motion Tokens
 *
 * Expressive motion: spring tabanlı, doğal hissettiren geçişler.
 * Klasik MD3'ten farkı: daha az tween, daha fazla spring.
 */
object SonaraMotion {

    // ── Spring specs ──────────────────────────────────────────
    /** Hızlı küçük UI hareketi (chip, badge) */
    val SpringFast = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness    = Spring.StiffnessHigh
    )

    /** Normal bileşen geçişi (kart, diyalog) */
    val SpringNormal = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness    = Spring.StiffnessMedium
    )

    /** Yavaş hero geçişi (bottom sheet, ekran geçişi) */
    val SpringSlow = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness    = Spring.StiffnessLow
    )

    // ── Tween specs (geriye dönük uyumluluk için) ─────────────
    val TweenEnter  = tween<Float>(durationMillis = 350, easing = EmphasizedDecelerate)
    val TweenExit   = tween<Float>(durationMillis = 200, easing = EmphasizedAccelerate)
    val TweenStd    = tween<Float>(durationMillis = 300, easing = Emphasized)

    // ── MD3 Expressive easing eğrileri ───────────────────────
    val Emphasized            = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val EmphasizedDecelerate  = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val EmphasizedAccelerate  = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
    val Standard              = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val StandardDecelerate    = CubicBezierEasing(0.0f, 0.0f, 0.0f, 1.0f)
    val StandardAccelerate    = CubicBezierEasing(0.3f, 0.0f, 1.0f, 1.0f)
}
