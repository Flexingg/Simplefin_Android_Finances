package com.randallengineering.finances.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val DuoGreen = Color(0xFF58CC02)
val DuoGreenDark = Color(0xFF46A302)
val DuoRed = Color(0xFFFF4B4B)
val DuoRedDark = Color(0xFFEA2B2B)
val DuoGold = Color(0xFFFFC800)
val DuoGoldDark = Color(0xFFE5A500)
val DuoBlue = Color(0xFF1CB0F6)
val DuoBlueDark = Color(0xFF1899D6)

@Composable
fun DuolingoPressableButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = DuoGreen,
    shadowColor: Color = DuoGreenDark,
    cornerRadius: Dp = 16.dp,
    shadowHeight: Dp = 4.dp,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val topOffset by animateDpAsState(
        targetValue = if (isPressed) shadowHeight else 0.dp,
        label = "ButtonPressOffset"
    )

    Box(
        modifier = modifier
            .padding(bottom = shadowHeight)
    ) {
        // Bottom 3D shadow layer
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = shadowHeight)
                .clip(RoundedCornerShape(cornerRadius))
                .background(if (enabled) shadowColor else Color.Gray.copy(alpha = 0.5f))
        )

        // Top interactive button surface
        Box(
            modifier = Modifier
                .offset(y = topOffset)
                .clip(RoundedCornerShape(cornerRadius))
                .background(if (enabled) backgroundColor else Color.Gray)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick
                )
                .padding(horizontal = 20.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                content = content
            )
        }
    }
}
