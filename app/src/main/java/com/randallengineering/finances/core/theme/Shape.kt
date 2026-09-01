package com.randallengineering.finances.core.theme

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Material Expressive: bold, mixed, slightly asymmetric radii for a modern,
// confident finance feel.
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(CornerSize(6.dp)),
    small = RoundedCornerShape(CornerSize(12.dp)),
    medium = RoundedCornerShape(
        topStart = CornerSize(18.dp),
        topEnd = CornerSize(18.dp),
        bottomEnd = CornerSize(18.dp),
        bottomStart = CornerSize(6.dp)
    ),
    large = RoundedCornerShape(
        topStart = CornerSize(28.dp),
        topEnd = CornerSize(28.dp),
        bottomEnd = CornerSize(28.dp),
        bottomStart = CornerSize(10.dp)
    ),
    extraLarge = RoundedCornerShape(CornerSize(40.dp))
)
