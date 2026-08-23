package com.randallengineering.finances.ui.screens.quest

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.randallengineering.finances.core.theme.Shapes
import com.randallengineering.finances.domain.model.QuestNode
import com.randallengineering.finances.domain.model.QuestNodeType
import com.randallengineering.finances.ui.components.DuoBlue
import com.randallengineering.finances.ui.components.DuoBlueDark
import com.randallengineering.finances.ui.components.DuoCardDark
import com.randallengineering.finances.ui.components.DuoCardShadow
import com.randallengineering.finances.ui.components.DuoGold
import com.randallengineering.finances.ui.components.DuoGoldDark
import com.randallengineering.finances.ui.components.DuoGreen
import com.randallengineering.finances.ui.components.DuoGreenDark
import com.randallengineering.finances.ui.components.DuoRed
import com.randallengineering.finances.ui.components.DuoRedDark
import com.randallengineering.finances.ui.components.DuolingoPressableButton
import com.randallengineering.finances.ui.components.GamificationHud
import org.koin.androidx.compose.koinViewModel
import kotlin.math.sin

@Composable
fun QuestPathScreen(
    viewModel: QuestPathViewModel = koinViewModel(),
    onNavigateToQueue: () -> Unit = {},
    onNavigateToRoute: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    // Node Detail Modal
    if (uiState.selectedNode != null) {
        val node = uiState.selectedNode!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissNodeDialog() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val emoji = when (node.nodeType) {
                        QuestNodeType.SAVINGS_CHEST -> "🎁"
                        QuestNodeType.BOSS_BATTLE -> "👾"
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
                                trackColor = MaterialTheme.colorScheme.surface
                            )
                        }
                    }

                    if (node.nodeType == QuestNodeType.BOSS_BATTLE) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = Shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = DuoRed.copy(alpha = 0.15f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Boss: ${node.bossName}", fontWeight = FontWeight.Bold, color = DuoRedDark)
                                Text("Remaining Budget HP: \$${node.bossCurrentHp.toInt()} / \$${node.bossMaxHp.toInt()}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Rewards:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        Text("+${node.rewardXp} XP  •  +${node.rewardGems} 💎", fontWeight = FontWeight.Bold, color = DuoGoldDark)
                    }
                }
            },
            confirmButton = {
                if (node.isCompleted) {
                    DuolingoPressableButton(
                        onClick = { viewModel.dismissNodeDialog() },
                        backgroundColor = DuoBlue,
                        shadowColor = DuoBlueDark,
                        cornerRadius = 10.dp
                    ) {
                        Text("Completed! ✅", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else if (node.isUnlocked && node.isCriteriaMet) {
                    DuolingoPressableButton(
                        onClick = { viewModel.claimNodeReward(node) },
                        backgroundColor = DuoGreen,
                        shadowColor = DuoGreenDark,
                        cornerRadius = 10.dp
                    ) {
                        Text("Claim +${node.rewardXp} XP! 🎉", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else if (node.isUnlocked) {
                    val destinationRoute = when (node.nodeType) {
                        QuestNodeType.SETUP_SIMPLEFIN -> com.randallengineering.finances.ui.navigation.Screen.Settings.route
                        QuestNodeType.SETUP_CATEGORIES, QuestNodeType.SETUP_SUBCATEGORIES, QuestNodeType.SETUP_BUDGETS, QuestNodeType.AUTO_RULES -> com.randallengineering.finances.ui.navigation.Screen.Budgets.route
                        QuestNodeType.SETUP_GOALS, QuestNodeType.SAVINGS_CHEST -> com.randallengineering.finances.ui.navigation.Screen.Goals.route
                        QuestNodeType.INBOX_ZERO, QuestNodeType.NOTE_BONUS, QuestNodeType.SPLIT_TRANSACTION -> com.randallengineering.finances.ui.navigation.Screen.ActionQueue.route
                        else -> com.randallengineering.finances.ui.navigation.Screen.Budgets.route
                    }

                    val buttonLabel = when (node.nodeType) {
                        QuestNodeType.SETUP_SIMPLEFIN -> "🚀 Go to Settings ➔"
                        QuestNodeType.SETUP_CATEGORIES, QuestNodeType.SETUP_SUBCATEGORIES -> "🚀 Go to Categories ➔"
                        QuestNodeType.SETUP_BUDGETS -> "🚀 Go to Budgets ➔"
                        QuestNodeType.AUTO_RULES -> "🚀 Go to Rules ➔"
                        QuestNodeType.SETUP_GOALS, QuestNodeType.SAVINGS_CHEST -> "🚀 Go to Goals ➔"
                        QuestNodeType.INBOX_ZERO, QuestNodeType.NOTE_BONUS, QuestNodeType.SPLIT_TRANSACTION -> "🚀 Go to Queue ➔"
                        else -> "🚀 Jump to Quest ➔"
                    }

                    DuolingoPressableButton(
                        onClick = {
                            viewModel.dismissNodeDialog()
                            onNavigateToRoute(destinationRoute)
                        },
                        backgroundColor = DuoGreen,
                        shadowColor = DuoGreenDark,
                        cornerRadius = 10.dp
                    ) {
                        Text(buttonLabel, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else {
                    DuolingoPressableButton(
                        onClick = { viewModel.dismissNodeDialog() },
                        backgroundColor = Color.Gray,
                        shadowColor = Color.DarkGray,
                        cornerRadius = 10.dp
                    ) {
                        Text("Locked 🔒", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                if (node.isUnlocked && !node.isCriteriaMet && !node.isCompleted) {
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

    val chapters = listOf(
        Pair(1, "1. Foundations"),
        Pair(2, "2. Review Habits"),
        Pair(3, "3. Budget Bosses")
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
                    selectedTabIndex = (uiState.selectedChapter - 1).coerceIn(0, 2),
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
                    else -> Quad(DuoRed, DuoRedDark, "CHAPTER 3: BUDGET MASTERY", "Keep weekly spending under limits and defeat Ignis the Takeout Wyrm & Chronos!")
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
                            Column {
                                Text(chapTitle, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = Color.White.copy(alpha = 0.8f))
                                Text(
                                    when (uiState.selectedChapter) {
                                        1 -> "App Setup & Setup Quests"
                                        2 -> "Transaction Reviewer"
                                        else -> "Budget Mastery & Bosses"
                                    },
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                            Icon(Icons.Default.MilitaryTech, contentDescription = null, tint = DuoGold, modifier = Modifier.size(36.dp))
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(chapSubtitle, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.9f))
                    }
                }
            }

            // S-Curve Nodes for Current Chapter
            itemsIndexed(currentChapterNodes) { index, node ->
                val xOffsetDp = (sin(index * 1.3) * 65.0).dp

                QuestNodeItem(
                    node = node,
                    isActive = node.id == uiState.activeNode?.id,
                    xOffset = xOffsetDp,
                    onClick = { viewModel.selectNode(node) }
                )

                if (index < currentChapterNodes.size - 1) {
                    Spacer(Modifier.height(18.dp))
                }
            }
        }
    }
}

data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
private fun QuestNodeItem(
    node: QuestNode,
    isActive: Boolean,
    xOffset: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "NodePulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isActive) 1.10f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    val (bgColor, shadowColor, icon) = when {
        node.isCompleted -> Triple(DuoGreen, DuoGreenDark, Icons.Default.Check)
        node.nodeType == QuestNodeType.SAVINGS_CHEST -> Triple(DuoGold, DuoGoldDark, Icons.Default.CardGiftcard)
        node.nodeType == QuestNodeType.BOSS_BATTLE -> Triple(DuoRed, DuoRedDark, Icons.Default.Star)
        node.isUnlocked && node.isCriteriaMet -> Triple(DuoGreen, DuoGreenDark, Icons.Default.AutoAwesome)
        node.isUnlocked -> Triple(DuoBlue, DuoBlueDark, Icons.Default.Star)
        else -> Triple(Color(0xFF2A2333), Color(0xFF1A1422), Icons.Default.Lock)
    }

    Box(
        modifier = Modifier
            .offset(x = xOffset)
            .scale(if (isActive) pulseScale else 1.0f)
            .size(76.dp),
        contentAlignment = Alignment.Center
    ) {
        DuolingoPressableButton(
            onClick = onClick,
            backgroundColor = bgColor,
            shadowColor = shadowColor,
            cornerRadius = 38.dp,
            shadowHeight = 6.dp,
            modifier = Modifier.size(72.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = node.title,
                tint = if (node.isUnlocked || node.isCompleted) Color.White else Color.Gray,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}
