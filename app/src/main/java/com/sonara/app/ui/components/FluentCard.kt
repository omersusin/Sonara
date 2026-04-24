package com.sonara.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sonara.app.ui.theme.SonaraCard

@Composable
fun FluentCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val base = modifier.fillMaxWidth()
    val clickMod = if (onClick != null) base.clickable { onClick() } else base
    Surface(modifier = clickMod, shape = MaterialTheme.shapes.large, color = SonaraCard, tonalElevation = 1.dp) {
        Column(modifier = Modifier.padding(18.dp), content = content)
    }
}
