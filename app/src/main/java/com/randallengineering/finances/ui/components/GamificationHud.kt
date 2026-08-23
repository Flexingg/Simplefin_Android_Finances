package com.randallengineering.finances.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.randallengineering.finances.core.theme.Shapes
import com.randallengineering.finances.data.repository.GamificationRepository
import org.koin.compose.koinInject

@Composable
fun GamificationHud(
    modifier: Modifier = Modifier,
    onQueueClick: (() -> Unit)? = null
) {
    val repository: GamificationRepository = koinInject()
    val state by repository.stateFlow.collectAsState()
    var showHeartsDialog by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "FlamePulse")
    val flameScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "FlameScale"
    )

    if (showHeartsDialog) {
        AlertDialog(
            onDismissRequest = { showHeartsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = DuoRed, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Budget Hearts (${state.hearts}/${state.maxHearts})", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "You lose a Heart when exceeding category budgets or daily spend allowances. Protect your streak by keeping your hearts full!",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    HorizontalDivider()

                    Text("❤️ Refill Quests:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                repository.refillHearts()
                                showHeartsDialog = false
                            },
                        shape = Shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Daily Budget Practice", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("Review your transaction queue", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            DuolingoPressableButton(
                                onClick = {
                                    repository.refillHearts()
                                    showHeartsDialog = false
                                },
                                backgroundColor = DuoGreen,
                                shadowColor = DuoGreenDark,
                                cornerRadius = 10.dp,
                                shadowHeight = 2.dp
                            ) {
                                Text("+5 ❤️ Refill", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                DuolingoPressableButton(
                    onClick = { showHeartsDialog = false },
                    backgroundColor = DuoBlue,
                    shadowColor = DuoBlueDark,
                    cornerRadius = 10.dp
                ) {
                    Text("Got it!", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Streak Counter
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(Shapes.small)
                        .background(DuoGold.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Default.LocalFireDepartment,
                        contentDescription = "Streak",
                        tint = Color(0xFFFF9600),
                        modifier = Modifier
                            .size(20.dp)
                            .scale(flameScale)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${state.streakDays}",
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFD97706)
                    )
                }

                // XP & Level Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(Shapes.small)
                        .background(DuoBlue.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Default.Bolt,
                        contentDescription = "XP",
                        tint = DuoBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${state.xp} XP",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = DuoBlueDark
                    )
                }

                // Hearts Counter (Clickable to Refill)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(Shapes.small)
                        .clickable { showHeartsDialog = true }
                        .background(DuoRed.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = "Hearts",
                        tint = DuoRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${state.hearts}",
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.titleMedium,
                        color = DuoRedDark
                    )
                    Spacer(Modifier.width(2.dp))
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add Hearts",
                        tint = DuoRedDark,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Level Progress Micro-Bar
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Lvl ${state.level} • ${state.levelTitle}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${state.xp % 250}/250 XP",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(3.dp))
            LinearProgressIndicator(
                progress = { state.levelProgressPercent },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = DuoGreen,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}
