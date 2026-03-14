package com.sonara.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.sonara.app.ui.theme.SonaraPrimary

@Composable
fun BandSlider(
    value: Float = 0f,
    onValueChange: (Float) -> Unit = {},
    label: String = "",
    bandColor: Color = SonaraPrimary
) {
    // Will be fully implemented with Equalizer screen
}
