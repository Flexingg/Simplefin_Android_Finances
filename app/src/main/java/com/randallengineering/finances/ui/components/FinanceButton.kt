package com.randallengineering.finances.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.randallengineering.finances.core.theme.FinanceGreen
import com.randallengineering.finances.core.theme.FinanceGreenDark

// A clean, flat Material button. shadowHeight/shadowColor are accepted for
// compatibility but ignored.
@Composable
fun FinanceButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = FinanceGreen,
    shadowColor: Color = FinanceGreenDark,
    cornerRadius: Dp = 16.dp,
    shadowHeight: Dp = 4.dp,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
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
        contentPadding = PaddingValues(16.dp),
        content = content
    )
}
