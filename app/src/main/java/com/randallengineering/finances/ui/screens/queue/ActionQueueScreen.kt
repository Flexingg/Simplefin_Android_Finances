package com.randallengineering.finances.ui.screens.queue

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.randallengineering.finances.core.theme.Shapes
import com.randallengineering.finances.core.util.CurrencyFormatter
import com.randallengineering.finances.core.util.DateUtils
import com.randallengineering.finances.domain.model.Transaction
import com.randallengineering.finances.ui.components.DuoBlue
import com.randallengineering.finances.ui.components.DuoBlueDark
import com.randallengineering.finances.ui.components.DuoGold
import com.randallengineering.finances.ui.components.DuoGoldDark
import com.randallengineering.finances.ui.components.DuoGreen
import com.randallengineering.finances.ui.components.DuoGreenDark
import com.randallengineering.finances.ui.components.DuoRed
import com.randallengineering.finances.ui.components.DuoRedDark
import com.randallengineering.finances.ui.components.DuolingoPressableButton
import com.randallengineering.finances.ui.components.GamificationHud
import org.koin.androidx.compose.koinViewModel
import kotlin.math.abs

private val QuickCategories = listOf(
    Pair("🍔 Dining", Pair("Dining", "Restaurants & Fast Food")),
    Pair("🛒 Groceries", Pair("Groceries", "Food & Pantry")),
    Pair("🚗 Auto & Gas", Pair("Automotive", "Fuel & Maintenance")),
    Pair("💡 Utilities", Pair("Utilities", "Electric, Gas & Internet")),
    Pair("🛍️ Shopping", Pair("Shopping", "Retail & Electronics")),
    Pair("🍿 Fun", Pair("Entertainment", "Movies & Recreation")),
    Pair("🏥 Health", Pair("Health & Medical", "Pharmacy & Wellness")),
    Pair("💼 Income", Pair("Income", "Salary & Deposits"))
)

@Composable
fun ActionQueueScreen(
    viewModel: ActionQueueViewModel = koinViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentTx = uiState.currentTransaction

    val isUncategorized = currentTx?.category.isNullOrBlank() || currentTx?.category.equals("Uncategorized", ignoreCase = true)

    Scaffold(
        topBar = {
            GamificationHud()
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 18.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Progress & Combo Status
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Daily Habit Review (${uiState.currentCardIndex}/${uiState.pendingTransactions.size})",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )

                    if (uiState.comboMultiplier > 1) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(Shapes.small)
                                .background(DuoGold.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = DuoGoldDark, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("${uiState.comboMultiplier}x Combo!", fontWeight = FontWeight.Black, color = DuoGoldDark, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                LinearProgressIndicator(
                    progress = {
                        if (uiState.pendingTransactions.isNotEmpty()) {
                            (uiState.currentCardIndex.toFloat() / uiState.pendingTransactions.size.toFloat()).coerceIn(0f, 1f)
                        } else 1f
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(Shapes.small),
                    color = DuoGreen,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            if (uiState.isSessionComplete || currentTx == null) {
                // Celebration Completion State
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    shape = Shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text("🦉", style = MaterialTheme.typography.displayLarge)
                        Text("Queue Cleared!", fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineMedium)
                        Text(
                            text = "You verified all transactions and left 0 uncategorized! +${uiState.totalXpEarnedInSession} XP earned today. Streak is secure.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(10.dp))

                        DuolingoPressableButton(
                            onClick = { viewModel.resetSession() },
                            backgroundColor = DuoGreen,
                            shadowColor = DuoGreenDark,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Replay, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Practice Again", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Duolingo Mascot Speech Prompt
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🦉", style = MaterialTheme.typography.headlineLarge)
                    Spacer(Modifier.width(10.dp))
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isUncategorized) DuoGold.copy(alpha = 0.18f) else DuoGreen.copy(alpha = 0.15f))
                    ) {
                        Text(
                            text = if (isUncategorized) "Choose a category below to earn +25 XP and avoid heart loss!" else "Is this category correct? Confirm or change it below!",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isUncategorized) DuoGoldDark else DuoGreenDark,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // Active Transaction Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = currentTx.payee.ifBlank { currentTx.originalDesc },
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.titleLarge,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Text(
                                text = DateUtils.formatDate(currentTx.postedEpochSeconds),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = CurrencyFormatter.format(abs(currentTx.amount)),
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.primary
                        )

                        HorizontalDivider()

                        // Current Category Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("Current Category: ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Card(
                                shape = Shapes.small,
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isUncategorized) DuoRed.copy(alpha = 0.15f) else DuoGreen.copy(alpha = 0.15f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isUncategorized) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = DuoRedDark, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = if (isUncategorized) "Uncategorized (Action Required)" else "${currentTx.category}${if (currentTx.subCategory.isNotBlank()) " > ${currentTx.subCategory}" else ""}",
                                        fontWeight = FontWeight.Bold,
                                        color = if (isUncategorized) DuoRedDark else DuoGreenDark,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }

                // 1-Tap Category Grid (Direct 3D buttons)
                Text(
                    text = if (isUncategorized) "Select Category to Earn XP:" else "Or Change Category:",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (i in QuickCategories.indices step 2) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val cat1 = QuickCategories[i]
                            DuolingoPressableButton(
                                onClick = { viewModel.editCategory(currentTx, cat1.second.first, cat1.second.second) },
                                backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                                shadowColor = Color.LightGray,
                                cornerRadius = 12.dp,
                                shadowHeight = 3.dp,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(cat1.first, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            }

                            if (i + 1 < QuickCategories.size) {
                                val cat2 = QuickCategories[i + 1]
                                DuolingoPressableButton(
                                    onClick = { viewModel.editCategory(currentTx, cat2.second.first, cat2.second.second) },
                                    backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                                    shadowColor = Color.LightGray,
                                    cornerRadius = 12.dp,
                                    shadowHeight = 3.dp,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(cat2.first, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }

                // Bottom Confirmation Button (Only enabled if categorized!)
                Spacer(Modifier.height(4.dp))
                DuolingoPressableButton(
                    onClick = { viewModel.confirmCategory(currentTx) },
                    enabled = !isUncategorized,
                    backgroundColor = DuoGreen,
                    shadowColor = DuoGreenDark,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isUncategorized) {
                        Text("⚠️ Choose Category Above to Confirm", color = Color.White, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Confirm (${currentTx.category}) +${15 * uiState.comboMultiplier} XP", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
