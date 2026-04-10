package com.sonara.app.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * MD3 Expressive Shape Scale
 *
 *  extraSmall  →  4 dp  (chip label, küçük badge)
 *  small       →  8 dp  (küçük buton, input)
 *  medium      → 16 dp  (kart, dialog)
 *  large       → 24 dp  (bottom sheet, büyük kart)
 *  extraLarge  → 32 dp  (modal bottom sheet başlığı, hero kart)
 *
 * Expressive fark: large+ radius çok daha büyük,
 * bileşenler "pill"e yaklaşır → yumuşak, canlı his.
 */
val SonaraShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small      = RoundedCornerShape(10.dp),
    medium     = RoundedCornerShape(18.dp),
    large      = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

// ─── Yardımcı şekiller ───────────────────────────────────────
/** Tam yuvarlak pill — FAB, chip, now-playing play butonu */
val PillShape = CircleShape

/** Asimetrik kart — üstte büyük, altta küçük radius */
val ExpressiveCardTop    = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 12.dp, bottomEnd = 12.dp)
val ExpressiveCardBottom = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 28.dp, bottomEnd = 28.dp)

/** Now Playing bar için özel şekil */
val NowPlayingShape = RoundedCornerShape(20.dp)

/** EQ slider göstergesi */
val SliderThumbShape = RoundedCornerShape(6.dp)

/** Bottom nav için yuvarlak indicator */
val NavIndicatorShape = RoundedCornerShape(50)
