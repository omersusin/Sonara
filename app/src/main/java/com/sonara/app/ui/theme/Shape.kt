package com.sonara.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// MD3 Expressive shape scale.
// extraSmall = pill (chips, tags, FABs)
// small      = buttons, text fields
// medium     = cards, dialogs
// large      = bottom sheets, nav drawers
// extraLarge = expressive asymmetric top-rounded panels
val SonaraShapes = Shapes(
    extraSmall = RoundedCornerShape(50),
    small      = RoundedCornerShape(12.dp),
    medium     = RoundedCornerShape(12.dp),
    large      = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(24.dp)
)
