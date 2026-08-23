package com.randallengineering.finances.ui.screens.goals

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.randallengineering.finances.core.theme.FinanceGreen
import com.randallengineering.finances.core.theme.Shapes
import com.randallengineering.finances.core.util.CurrencyFormatter
import com.randallengineering.finances.core.util.DateUtils
import com.randallengineering.finances.domain.model.Goal
import com.randallengineering.finances.domain.usecase.GoalPacingResult
import com.randallengineering.finances.ui.components.ExpressiveCard
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@Composable
fun GoalsAndWantsScreen(
    viewModel: GoalsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.isCreatingGoal || uiState.editingGoal != null) {
        GoalEditorDialog(
            initialGoal = uiState.editingGoal,
            onDismiss = { viewModel.closeDialog() },
            onSave = { viewModel.saveGoal(it) }
        )
    }

    if (uiState.contributingGoal != null) {
        ContributeGoalDialog(
            goal = uiState.contributingGoal!!,
            onDismiss = { viewModel.closeDialog() },
            onContribute = { amount ->
                viewModel.contributeToGoal(uiState.contributingGoal!!, amount)
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openCreateGoalDialog() },
                shape = Shapes.medium,
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Goal")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            // Header summary
            ExpressiveCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = Shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Savings, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Savings & Wants Targets",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Tracks daily and monthly required saving velocity to achieve milestones by target dates.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                    )
                }
            }

            if (uiState.isLoading && uiState.goals.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.goals.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No financial goals created yet.\nTap '+' below to add an emergency fund, vacation, or purchase target!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = uiState.goals,
                        key = { it.goal.id }
                    ) { pacingResult ->
                        GoalItemCard(
                            pacing = pacingResult,
                            onContribute = { viewModel.openContributeDialog(pacingResult.goal) },
                            onEdit = { viewModel.openEditGoalDialog(pacingResult.goal) },
                            onDelete = { viewModel.deleteGoal(pacingResult.goal.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GoalItemCard(
    pacing: GoalPacingResult,
    onContribute: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val goal = pacing.goal
    val animatedProgress by animateFloatAsState(
        targetValue = (pacing.progressPercent / 100.0).toFloat(),
        label = "goal_progress"
    )

    ExpressiveCard(
        modifier = Modifier.fillMaxWidth(),
        shape = Shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = goal.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (goal.isCompleted) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Default.CheckCircle, contentDescription = "Completed", tint = FinanceGreen, modifier = Modifier.size(18.dp))
                    }
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = "${CurrencyFormatter.format(goal.currentAmount)} of ${CurrencyFormatter.format(goal.targetAmount)} (${String.format("%.1f%%", pacing.progressPercent)})",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = if (goal.isCompleted) FinanceGreen else MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(12.dp))

            // Saving Velocity Required
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Target Date: ${DateUtils.formatDate(goal.targetEpochSeconds)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Req: ${CurrencyFormatter.format(pacing.requiredDailySaving)}/day • ${CurrencyFormatter.format(pacing.requiredMonthlySaving)}/mo",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                OutlinedButton(
                    onClick = onContribute,
                    shape = Shapes.small
                ) {
                    Text("+ Add Funds")
                }
            }
        }
    }
}

@Composable
fun GoalEditorDialog(
    initialGoal: Goal? = null,
    onDismiss: () -> Unit,
    onSave: (Goal) -> Unit
) {
    var title by remember { mutableStateOf(initialGoal?.title.orEmpty()) }
    var targetAmountText by remember { mutableStateOf(initialGoal?.targetAmount?.toString().orEmpty()) }
    var currentAmountText by remember { mutableStateOf(initialGoal?.currentAmount?.toString().orEmpty()) }
    var monthsAheadText by remember { mutableStateOf("6") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = Shapes.large,
        title = {
            Text(
                text = if (initialGoal == null) "Create Savings Goal" else "Edit Goal",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Goal Title") },
                    placeholder = { Text("e.g. Emergency Fund, New Laptop") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.small,
                    singleLine = true
                )

                OutlinedTextField(
                    value = targetAmountText,
                    onValueChange = { targetAmountText = it },
                    label = { Text("Target Amount ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.small,
                    singleLine = true
                )

                OutlinedTextField(
                    value = currentAmountText,
                    onValueChange = { currentAmountText = it },
                    label = { Text("Initial Saved Amount ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.small,
                    singleLine = true
                )

                OutlinedTextField(
                    value = monthsAheadText,
                    onValueChange = { monthsAheadText = it },
                    label = { Text("Target Timeline (Months from now)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.small,
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val months = monthsAheadText.toLongOrNull() ?: 6L
                    val targetEpoch = LocalDate.now().plusMonths(months).atStartOfDay(ZoneId.systemDefault()).toEpochSecond()

                    val targetAmt = targetAmountText.toDoubleOrNull() ?: 0.0
                    val currentAmt = currentAmountText.toDoubleOrNull() ?: 0.0

                    val goal = Goal(
                        id = initialGoal?.id ?: UUID.randomUUID().toString(),
                        title = title.ifBlank { "Savings Goal" },
                        targetAmount = targetAmt,
                        currentAmount = currentAmt,
                        targetEpochSeconds = initialGoal?.targetEpochSeconds ?: targetEpoch,
                        isCompleted = currentAmt >= targetAmt && targetAmt > 0
                    )
                    onSave(goal)
                },
                enabled = title.isNotBlank() && targetAmountText.toDoubleOrNull() != null,
                shape = Shapes.small
            ) {
                Text("Save Goal")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = Shapes.small) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ContributeGoalDialog(
    goal: Goal,
    onDismiss: () -> Unit,
    onContribute: (Double) -> Unit
) {
    var amountText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = Shapes.large,
        title = {
            Text("Contribute to ${goal.title}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Current: ${CurrencyFormatter.format(goal.currentAmount)} / ${CurrencyFormatter.format(goal.targetAmount)}")
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Contribution Amount ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.small,
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount > 0) {
                        onContribute(amount)
                    }
                },
                enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0,
                shape = Shapes.small
            ) {
                Text("Add Contribution")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = Shapes.small) {
                Text("Cancel")
            }
        }
    )
}
