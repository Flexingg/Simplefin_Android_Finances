package com.randallengineering.finances.ui.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.ButtonDefaults
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.randallengineering.finances.MainActivity

private fun cp(color: androidx.compose.ui.graphics.Color) =
    ColorProvider(day = color, night = color)

class RandallFinancesWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                WidgetContent()
            }
        }
    }

    @Composable
    private fun WidgetContent() {
        val cardColor = androidx.compose.ui.graphics.Color(0xFF14181B)
        val accent = androidx.compose.ui.graphics.Color(0xFF1B873F)

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(cardColor)
                .cornerRadius(16.dp)
                .padding(14.dp)
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Text(
                    text = "Randall Finances",
                    style = TextStyle(
                        color = cp(androidx.compose.ui.graphics.Color.White),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                )

                Spacer(modifier = GlanceModifier.height(6.dp))

                Text(
                    text = "Open the app to review and manage your transactions.",
                    style = TextStyle(
                        color = cp(androidx.compose.ui.graphics.Color(0xFFB9C2C6)),
                        fontSize = 12.sp
                    )
                )

                Spacer(modifier = GlanceModifier.height(12.dp))

                Button(
                    text = "Open App",
                    onClick = actionStartActivity<MainActivity>(),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = cp(accent),
                        contentColor = cp(androidx.compose.ui.graphics.Color.White)
                    ),
                    modifier = GlanceModifier.fillMaxWidth().height(36.dp)
                )
            }
        }
    }
}

class RandallFinancesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RandallFinancesWidget()
}
