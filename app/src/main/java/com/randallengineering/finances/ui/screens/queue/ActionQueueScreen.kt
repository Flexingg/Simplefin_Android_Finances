package com.randallengineering.finances.ui.screens.queue

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Replay
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
import com.randallengineering.finances.ui.components.CategoryPickerDialog
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

@Composable
fun ActionQueueScreen(
    viewModel: ActionQueueViewModel = koinViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var isEditingCategory by remember { mutableStateOf(false) }

    val currentTx = uiState.currentTransaction

    val categoryRepository: com.randallengineering.finances.data.repository.CategoryRepository = org.koin.compose.koinInject()
    val categories by categoryRepository.getCategoriesFlow().collectAsState(initial = com.randallengineering.finances.core.network.Resource.Success(emptyList()))
    val categoryList = (categories as? com.randallengineering.finances.core.network.Resource.Success)?.data ?: emptyList()

    if (isEditingCategory && currentTx != null) {
        CategoryPickerDialog(
            categories = categoryList,
            initialMainCategory = currentTx.category,
            initialSubCategory = currentTx.subCategory,
            onDismiss = { isEditingCategory = false },
            onCategorySelected = { cat, sub ->
                viewModel.editCategory(currentTx, cat, sub)
                isEditingCategory = false
            },
            onAddNewCategory = { main, sub ->
                // Add new category
                isEditingCategory = false
            }
        )
    }

    Scaffold(
        topBar = {
            GamificationHud()
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
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
                        text = "Daily Review Queue (${uiState.currentCardIndex}/${uiState.pendingTransactions.size})",
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

            // Main Flashcard View
            if (uiState.isSessionComplete || currentTx == null) {
                // Celebration Completion State
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    shape = Shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text("🎉", style = MaterialTheme.typography.displayLarge)
                        Text("Queue Cleared!", fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineMedium)
                        Text(
                            text = "You verified all transactions and earned +${uiState.totalXpEarnedInSession} XP today! Your budget streak is secure.",
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
                // Active Transaction Review Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    shape = Shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(Shapes.medium)
                                .background(DuoBlue.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CreditCard, contentDescription = null, tint = DuoBlue, modifier = Modifier.size(30.dp))
                        }

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

                        // Auto-Categorized Category Chip
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Auto-Detected Category:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Card(
                                shape = Shapes.small,
                                colors = CardDefaults.cardColors(containerColor = DuoGreen.copy(alpha = 0.15f))
                            ) {
                                Text(
                                    text = "🏷️ ${currentTx.category}${if (currentTx.subCategory.isNotBlank()) " > ${currentTx.subCategory}" else ""}",
                                    fontWeight = FontWeight.Bold,
                                    color = DuoGreenDark,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Action Buttons
            if (!uiState.isSessionComplete && currentTx != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DuolingoPressableButton(
                        onClick = { isEditingCategory = true },
                        backgroundColor = DuoBlue,
                        shadowColor = DuoBlueDark,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text("Edit", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    DuolingoPressableButton(
                        onClick = { viewModel.confirmCategory(currentTx) },
                        backgroundColor = DuoGreen,
                        shadowColor = DuoGreenDark,
                        modifier = Modifier.weight(1.6f)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text("Confirm (+${15 * uiState.comboMultiplier} XP)", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}
