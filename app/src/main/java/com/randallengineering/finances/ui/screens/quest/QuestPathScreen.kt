package com.randallengineering.finances.ui.screens.quest

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.randallengineering.finances.core.audio.DuolingoSoundEffects
import com.randallengineering.finances.core.theme.Shapes
import com.randallengineering.finances.core.util.CurrencyFormatter
import com.randallengineering.finances.domain.model.CategoryHierarchy
import com.randallengineering.finances.domain.model.CustomQuestChallenge
import com.randallengineering.finances.domain.model.QuestNode
import com.randallengineering.finances.domain.model.QuestNodeType
import com.randallengineering.finances.ui.components.*
import org.koin.androidx.compose.koinViewModel
import java.util.UUID

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun QuestPathScreen(
    viewModel: QuestPathViewModel = koinViewModel(),
    onNavigateToQueue: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToBudgets: () -> Unit = {},
    onNavigateToGoals: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    // Node Detail Bottom Sheet / Alert Modal
    if (uiState.selectedNode != null) {
        val node = uiState.selectedNode!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissNodeDialog() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val emoji = when (node.nodeType) {
                        QuestNodeType.BOSS_BATTLE -> "🐉"
                        QuestNodeType.SAVINGS_CHEST -> "🎁"
                        QuestNodeType.SETUP_SIMPLEFIN -> "🏦"
                        QuestNodeType.SETUP_CATEGORIES -> "🗂️"
                        QuestNodeType.SETUP_SUBCATEGORIES -> "📑"
                        QuestNodeType.SETUP_BUDGETS -> "📊"
                        QuestNodeType.SETUP_GOALS -> "🎯"
                        QuestNodeType.SPLIT_TRANSACTION -> "✂️"
                        QuestNodeType.NOTE_BONUS -> "💬"
                        QuestNodeType.AUTO_RULES -> "⚡"
                        QuestNodeType.ZERO_SPEND -> "🛡️"
                        QuestNodeType.INBOX_ZERO -> "📬"
                        QuestNodeType.CUSTOM_CHALLENGE -> "⚔️"
                        else -> "⭐"
                    }
                    Text(emoji, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.width(8.dp))
                    Text(node.title, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(node.subtitle, style = MaterialTheme.typography.bodyMedium)

                    // Requirement Description & Live Real-Time Progress
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = Shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Requirement:",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = node.requirementDescription,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Live Status:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                Text(node.progressText, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, color = if (node.isCriteriaMet) DuoGreenDark else MaterialTheme.colorScheme.primary)
                            }

                            LinearProgressIndicator(
                                progress = { node.progressPercent },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape),
                                color = if (node.isCriteriaMet) DuoGreen else DuoBlue,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }

                    // Boss Battle HP Bar
                    if (node.nodeType == QuestNodeType.BOSS_BATTLE) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = Shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = DuoRedDark.copy(alpha = 0.15f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "⚔️ ${node.bossName ?: "Boss Monster"}",
                                    fontWeight = FontWeight.Black,
                                    color = DuoRedDark,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "HP: ${CurrencyFormatter.format(node.bossCurrentHp)} / ${CurrencyFormatter.format(node.bossMaxHp)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                                LinearProgressIndicator(
                                    progress = { if (node.bossMaxHp > 0) (node.bossCurrentHp.toFloat() / node.bossMaxHp.toFloat()).coerceIn(0f, 1f) else 0f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(CircleShape),
                                    color = DuoRed,
                                    trackColor = Color(0xFF3E1212)
                                )
                            }
                        }
                    }

                    // Reward Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Rewards:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("⚡ +${node.rewardXp} XP", fontWeight = FontWeight.Black, color = DuoGoldDark)
                            Text("💎 +${node.rewardGems} Gems", fontWeight = FontWeight.Black, color = DuoBlueDark)
                        }
                    }
                }
            },
            confirmButton = {
                if (node.isCompleted) {
                    DuolingoPressableButton(
                        onClick = { viewModel.dismissNodeDialog() },
                        backgroundColor = DuoCardDark,
                        shadowColor = DuoCardShadow,
                        cornerRadius = 10.dp
                    ) {
                        Text("Completed! ✅", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else if (node.isUnlocked && node.isCriteriaMet) {
                    DuolingoPressableButton(
                        onClick = {
                            DuolingoSoundEffects.playSuccessChime(context)
                            viewModel.claimNodeReward(node)
                        },
                        backgroundColor = DuoGreen,
                        shadowColor = DuoGreenDark,
                        cornerRadius = 10.dp
                    ) {
                        Text("Claim +${node.rewardXp} XP! 🎉", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else if (node.isUnlocked) {
                    val destinationRoute = when (node.nodeType) {
                        QuestNodeType.SETUP_SIMPLEFIN -> onNavigateToSettings
                        QuestNodeType.SETUP_CATEGORIES, QuestNodeType.SETUP_SUBCATEGORIES, QuestNodeType.SETUP_BUDGETS, QuestNodeType.AUTO_RULES -> onNavigateToBudgets
                        QuestNodeType.SETUP_GOALS, QuestNodeType.SAVINGS_CHEST -> onNavigateToGoals
                        QuestNodeType.INBOX_ZERO, QuestNodeType.SPLIT_TRANSACTION, QuestNodeType.NOTE_BONUS -> onNavigateToQueue
                        else -> null
                    }

                    if (destinationRoute != null) {
                        DuolingoPressableButton(
                            onClick = {
                                viewModel.dismissNodeDialog()
                                destinationRoute()
                            },
                            backgroundColor = DuoBlue,
                            shadowColor = DuoBlueDark,
                            cornerRadius = 10.dp
                        ) {
                            Text("Go to Feature ➔", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        DuolingoPressableButton(
                            onClick = { viewModel.dismissNodeDialog() },
                            backgroundColor = DuoCardDark,
                            shadowColor = DuoCardShadow,
                            cornerRadius = 10.dp
                        ) {
                            Text("Keep Budgeting!", color = Color.White)
                        }
                    }
                } else {
                    DuolingoPressableButton(
                        onClick = { viewModel.dismissNodeDialog() },
                        backgroundColor = DuoCardDark,
                        shadowColor = DuoCardShadow,
                        cornerRadius = 10.dp
                    ) {
                        Text("Locked (Complete Previous)", color = Color.White.copy(alpha = 0.6f))
                    }
                }
            },
            dismissButton = {
                if (!node.isCompleted) {
                    DuolingoPressableButton(
                        onClick = { viewModel.dismissNodeDialog() },
                        backgroundColor = DuoCardDark,
                        shadowColor = DuoCardShadow,
                        cornerRadius = 10.dp
                    ) {
                        Text("Close", color = Color.White)
                    }
                }
            }
        )
    }

    // Custom Quest Creation Modal
    if (uiState.isCreatingCustomQuest) {
        DuolingoCreateCustomQuestDialog(
            categories = uiState.categories,
            onDismiss = { viewModel.closeCreateCustomQuestDialog() },
            onSave = { viewModel.saveCustomQuest(it) }
        )
    }

    val chapters = listOf(
        Pair(1, "1. Foundations"),
        Pair(2, "2. Review Habits"),
        Pair(3, "3. Budget Bosses"),
        Pair(4, "4. ⚔️ Custom Arena")
    )

    val currentChapterNodes = uiState.nodes.filter { it.chapter == uiState.selectedChapter }

    Scaffold(
        topBar = {
            GamificationHud(onQueueClick = onNavigateToQueue)
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Chapter Selection Tabs
            item {
                ScrollableTabRow(
                    selectedTabIndex = (uiState.selectedChapter - 1).coerceIn(0, 3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    edgePadding = 0.dp
                ) {
                    chapters.forEach { (chapNum, title) ->
                        Tab(
                            selected = uiState.selectedChapter == chapNum,
                            onClick = { viewModel.selectChapter(chapNum) },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = if (uiState.selectedChapter == chapNum) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }
            }

            // Chapter Header Banner
            item {
                val (bannerBg, bannerShadow, chapTitle, chapSubtitle) = when (uiState.selectedChapter) {
                    1 -> Quad(DuoBlue, DuoBlueDark, "CHAPTER 1: FOUNDATIONS", "Set up your SimpleFIN link, 6 main categories, 25 subcategories, and first budget!")
                    2 -> Quad(DuoGreen, DuoGreenDark, "CHAPTER 2: THE REVIEWER", "Categorize daily transactions, use Quick Split, attach notes, and maintain your streak!")
                    3 -> Quad(DuoRed, DuoRedDark, "CHAPTER 3: BUDGET MASTERY", "Keep weekly spending under limits and defeat The Compound Colossus 🐉!")
                    else -> Quad(DuoGold, DuoGoldDark, "CHAPTER 4: CUSTOM ARENA", "Craft custom quests, set personalized budget boss battles, and earn custom rewards!")
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = Shapes.large,
                    colors = CardDefaults.cardColors(containerColor = bannerBg)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = chapTitle,
                                color = Color.White.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                letterSpacing = 1.sp
                            )
                            val completedCount = currentChapterNodes.count { it.isCompleted }
                            Text(
                                text = "$completedCount / ${currentChapterNodes.size} Done",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = chapSubtitle,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        if (uiState.selectedChapter == 4) {
                            Spacer(Modifier.height(12.dp))
                            DuolingoPressableButton(
                                onClick = { viewModel.openCreateCustomQuestDialog() },
                                backgroundColor = DuoGreen,
                                shadowColor = DuoGreenDark,
                                cornerRadius = 10.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Text("Create Custom Quest / Boss Battle", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (uiState.selectedChapter == 4 && currentChapterNodes.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = Shapes.large,
                        colors = CardDefaults.cardColors(containerColor = DuoCardDark)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("⚔️", style = MaterialTheme.typography.displaySmall)
                            Text("No Custom Quests Created", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Text("Tap 'Create Custom Quest / Boss Battle' above to craft your own financial challenge!", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                }
            }

            // Render Nodes Along Vertical Duolingo Stepping-Stone Path
            itemsIndexed(currentChapterNodes) { index, node ->
                val xOffsets = listOf(0.dp, 45.dp, 75.dp, 45.dp, 0.dp, (-45).dp, (-75).dp, (-45).dp)
                val currentOffset = xOffsets[index % xOffsets.size]

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Stepping Stone Node
                    Box(
                        modifier = Modifier
                            .offset(x = currentOffset)
                            .padding(vertical = 12.dp)
                    ) {
                        DuolingoQuestStone(
                            node = node,
                            onClick = { viewModel.selectNode(node) }
                        )
                    }

                    // Stepping Stone Connector Dots
                    if (index < currentChapterNodes.size - 1) {
                        val nextOffset = xOffsets[(index + 1) % xOffsets.size]
                        val midOffset = (currentOffset + nextOffset) / 2
                        Canvas(
                            modifier = Modifier
                                .height(28.dp)
                                .width(60.dp)
                                .offset(x = midOffset)
                        ) {
                            val pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                            drawLine(
                                color = if (node.isCompleted) Color(0xFF58CC02) else Color.White.copy(alpha = 0.2f),
                                start = Offset(size.width / 2, 0f),
                                end = Offset(size.width / 2, size.height),
                                strokeWidth = 8f,
                                cap = StrokeCap.Round,
                                pathEffect = pathEffect
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DuolingoQuestStone(
    node: QuestNode,
    onClick: () -> Unit
) {
    val (bgColor, shadowColor) = when {
        node.isCompleted -> Pair(DuoGreen, DuoGreenDark)
        node.isUnlocked && node.isCriteriaMet -> Pair(DuoGold, DuoGoldDark)
        node.isUnlocked -> Pair(DuoBlue, DuoBlueDark)
        else -> Pair(Color(0xFFE5E5E5), Color(0xFFAFAFAF))
    }

    val emoji = when (node.nodeType) {
        QuestNodeType.BOSS_BATTLE -> "🐉"
        QuestNodeType.SAVINGS_CHEST -> "🎁"
        QuestNodeType.SETUP_SIMPLEFIN -> "🏦"
        QuestNodeType.SETUP_CATEGORIES -> "🗂️"
        QuestNodeType.SETUP_SUBCATEGORIES -> "📑"
        QuestNodeType.SETUP_BUDGETS -> "📊"
        QuestNodeType.SETUP_GOALS -> "🎯"
        QuestNodeType.SPLIT_TRANSACTION -> "✂️"
        QuestNodeType.NOTE_BONUS -> "💬"
        QuestNodeType.AUTO_RULES -> "⚡"
        QuestNodeType.ZERO_SPEND -> "🛡️"
        QuestNodeType.INBOX_ZERO -> "📬"
        QuestNodeType.CUSTOM_CHALLENGE -> "⚔️"
        else -> "⭐"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        DuolingoPressableButton(
            onClick = onClick,
            backgroundColor = bgColor,
            shadowColor = shadowColor,
            cornerRadius = 35.dp,
            modifier = Modifier.size(70.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (node.isCompleted) {
                    Icon(Icons.Default.Check, contentDescription = "Completed", tint = Color.White, modifier = Modifier.size(32.dp))
                } else if (!node.isUnlocked) {
                    Icon(Icons.Default.Lock, contentDescription = "Locked", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(24.dp))
                } else {
                    Text(emoji, fontSize = 28.sp)
                }
            }
        }

        Text(
            text = node.title,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

// -------------------------------------------------------------
// Custom Quest & Boss Battle Creator Dialog
// -------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DuolingoCreateCustomQuestDialog(
    categories: List<CategoryHierarchy>,
    onDismiss: () -> Unit,
    onSave: (CustomQuestChallenge) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull()?.mainCategory ?: "Dining") }
    var limitAmountText by remember { mutableStateOf("150.00") }
    var isBossBattle by remember { mutableStateOf(false) }
    var bossName by remember { mutableStateOf("The Takeout Wyrm 🐉") }
    var rewardXp by remember { mutableStateOf("250") }
    var rewardGems by remember { mutableStateOf("50") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("⚔️ Create Custom Quest / Boss", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Quest / Challenge Title (e.g. Coffee Restraint)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Category Selection
                Text("Target Category:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.take(8).forEach { cat ->
                        val isSelected = selectedCategory.equals(cat.mainCategory, ignoreCase = true)
                        Card(
                            modifier = Modifier
                                .clip(Shapes.small)
                                .clickable { selectedCategory = cat.mainCategory },
                            colors = CardDefaults.cardColors(containerColor = if (isSelected) DuoGreen else DuoCardDark)
                        ) {
                            Text(
                                text = cat.mainCategory,
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = limitAmountText,
                    onValueChange = { limitAmountText = it },
                    label = { Text(if (isBossBattle) "Boss Max HP / Budget Limit ($)" else "Monthly Spending Limit ($)") },
                    prefix = { Text("$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🐉 Make this a Boss Battle?", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = isBossBattle,
                        onCheckedChange = { isBossBattle = it }
                    )
                }

                if (isBossBattle) {
                    OutlinedTextField(
                        value = bossName,
                        onValueChange = { bossName = it },
                        label = { Text("Boss Name (e.g. The Amazon Kraken 🐙)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            DuolingoPressableButton(
                onClick = {
                    val limit = limitAmountText.toDoubleOrNull() ?: 150.0
                    val xp = rewardXp.toIntOrNull() ?: 200
                    val gems = rewardGems.toIntOrNull() ?: 50

                    if (title.isNotBlank() && limit > 0) {
                        val quest = CustomQuestChallenge(
                            id = "custom_quest_${UUID.randomUUID()}",
                            title = title.trim(),
                            subtitle = if (isBossBattle) "Defeat $bossName by staying under $$limit in $selectedCategory!" else "Keep monthly $selectedCategory under $$limit",
                            category = selectedCategory,
                            targetAmount = limit,
                            rewardXp = xp,
                            rewardGems = gems,
                            isBossBattle = isBossBattle,
                            bossName = bossName.trim()
                        )
                        onSave(quest)
                    }
                },
                backgroundColor = DuoGreen,
                shadowColor = DuoGreenDark,
                cornerRadius = 10.dp
            ) {
                Text("Spawn Quest ➔", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            DuolingoPressableButton(
                onClick = onDismiss,
                backgroundColor = DuoCardDark,
                shadowColor = DuoCardShadow,
                cornerRadius = 10.dp
            ) {
                Text("Cancel", color = Color.White)
            }
        }
    )
}
