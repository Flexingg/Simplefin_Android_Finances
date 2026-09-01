package com.randallengineering.finances.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Finance-palette aliases (kept so existing call sites stay valid after the
// de-gamification; these are no longer the bright Duolingo greens).
val DuoGreen = Color(0xFF1B873F)
val DuoGreenDark = Color(0xFF146B31)
val DuoRed = Color(0xFFC62828)
val DuoRedDark = Color(0xFF8E1B1B)
val DuoGold = Color(0xFFB8860B)
val DuoGoldDark = Color(0xFF8A6508)
val DuoBlue = Color(0xFF1565C0)
val DuoBlueDark = Color(0xFF0D47A1)
val DuoCardDark = Color(0xFF2B3136)
val DuoCardShadow = Color(0xFF1C2125)

// A clean, flat Material button (the previous 3D "press-down" Duolingo look is
// gone). The extra shadowHeight/shadowColor params are accepted for
// compatibility but ignored.
@Composable
fun DuolingoPressableButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = DuoGreen,
    shadowColor: Color = DuoGreenDark,
    cornerRadius: Dp = 16.dp,
    shadowHeight: Dp = 4.dp,
    enabled: Boolean = true,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(cornerRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            disabledContainerColor = Color(0xFF2C2C2C),
            contentColor = Color.White
        ),
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        content = content
    )
}
