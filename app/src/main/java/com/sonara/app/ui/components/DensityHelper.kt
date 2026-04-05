package com.sonara.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Helper object to avoid Kotlin compiler bug with LocalDensity.current inlining.
 * See: https://youtrack.jetbrains.com/issue/CMP-9361
 */
object DensityHelper {
    @Composable
    fun dpToPx(dp: Dp): Float {
        return LocalDensity.current.run { dp.toPx() }
    }

    @Composable
    fun pxToDp(px: Float): Dp {
        return LocalDensity.current.run { px.toDp() }
    }
}
