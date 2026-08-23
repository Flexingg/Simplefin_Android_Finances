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
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.randallengineering.finances.MainActivity

class RandallFinancesWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Read cached gamification state & pending review count
        val prefs = context.getSharedPreferences("randall_finances_prefs", Context.MODE_PRIVATE)
        val streak = prefs.getInt("widget_streak", 3)
        val level = prefs.getInt("widget_level", 1)
        val pendingCount = prefs.getInt("widget_pending_queue", 4)

        provideContent {
            GlanceTheme {
                WidgetContent(streak = streak, level = level, pendingCount = pendingCount)
            }
        }
    }

    @Composable
    private fun WidgetContent(streak: Int, level: Int, pendingCount: Int) {
        val darkCardColor = androidx.compose.ui.graphics.Color(0xFF1E1726)
        val greenColor = androidx.compose.ui.graphics.Color(0xFF58CC02)
        val goldColor = androidx.compose.ui.graphics.Color(0xFFFFC800)

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(darkCardColor)
                .cornerRadius(16.dp)
                .padding(14.dp)
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                // Top Row: Mascot, Level, and Streak
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Text(
                        text = "🦉 Level $level",
                        style = TextStyle(
                            color = ColorProvider(greenColor),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Text(
                        text = "🔥 $streak Days",
                        style = TextStyle(
                            color = ColorProvider(goldColor),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                // Queue Review Prompt
                Text(
                    text = if (pendingCount > 0) "📬 $pendingCount Transactions to Review (+XP)" else "🎉 Queue Cleared! Inbox Zero!",
                    style = TextStyle(
                        color = ColorProvider(androidx.compose.ui.graphics.Color.White),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                )

                Spacer(modifier = GlanceModifier.height(10.dp))

                // 1-Tap Quick Review Button
                Button(
                    text = "⚡ Review Daily Queue ➔",
                    onClick = actionStartActivity<MainActivity>(),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = ColorProvider(greenColor),
                        contentColor = ColorProvider(androidx.compose.ui.graphics.Color.White)
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
