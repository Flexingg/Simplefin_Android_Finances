package com.randallengineering.finances.ui.screens.ai

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.randallengineering.finances.core.theme.FinanceGreen
import com.randallengineering.finances.core.theme.FinancePurple
import com.randallengineering.finances.core.theme.Shapes
import com.randallengineering.finances.core.util.CurrencyFormatter
import com.randallengineering.finances.domain.model.AiInsight
import com.randallengineering.finances.domain.model.InsightSeverity
import com.randallengineering.finances.domain.model.InsightType
import com.randallengineering.finances.ui.components.ExpressiveCard
import com.randallengineering.finances.ui.components.SnapshotExportSheet
import org.koin.androidx.compose.koinViewModel

@Composable
fun AiAdvisorScreen(
    viewModel: AiAdvisorViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.isSnapshotSheetOpen && uiState.snapshot != null) {
        SnapshotExportSheet(
            snapshot = uiState.snapshot!!,
            onDismiss = { viewModel.closeSnapshotSheet() }
        )
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.openSnapshotSheet() },
                shape = Shapes.medium,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            ) {
                Icon(Icons.Default.DataObject, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Copy AI Snapshot (MCP)")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Banner
            ExpressiveCard(
                modifier = Modifier.fillMaxWidth(),
                shape = Shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "AI Financial Advisor (Gemini 3.7 Flash)",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Real-time anomaly detection, pacing analysis, cash flow forecasting, and instant context export for AI/MCP tools.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )

                    Spacer(Modifier.height(14.dp))

                    Button(
                        onClick = { viewModel.generateGeminiAnalysis() },
                        enabled = !uiState.isGeneratingGemini && uiState.snapshot != null,
                        shape = Shapes.small
                    ) {
                        if (uiState.isGeneratingGemini) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Gemini is analyzing your finances...")
                        } else {
                            Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Run Full Financial Diagnosis")
                        }
                    }
                }
            }

            // Real-Time Anomaly & Cash Flow Alerts
            Text(
                text = "⚡ Automated Financial Alerts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (uiState.ruleBasedInsights.isEmpty()) {
                ExpressiveCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = FinanceGreen)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "All budget categories are pacing normally under 120%. Great job!",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                uiState.ruleBasedInsights.forEach { insight ->
                    InsightCard(insight = insight)
                }
            }

            // Gemini Deep Insights View
            if (uiState.geminiAnalysisMarkdown != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "🧠 Gemini 3.7 Flash Analysis",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                ExpressiveCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Text(
                            text = uiState.geminiAnalysisMarkdown ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
                        )
                    }
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
fun InsightCard(insight: AiInsight) {
    val isAnomaly = insight.type == InsightType.ANOMALY_OVERPACING
    val containerColor = when (insight.severity) {
        InsightSeverity.HIGH -> MaterialTheme.colorScheme.errorContainer
        InsightSeverity.MEDIUM -> MaterialTheme.colorScheme.secondaryContainer
        InsightSeverity.LOW -> MaterialTheme.colorScheme.tertiaryContainer
    }

    val contentColor = when (insight.severity) {
        InsightSeverity.HIGH -> MaterialTheme.colorScheme.onErrorContainer
        InsightSeverity.MEDIUM -> MaterialTheme.colorScheme.onSecondaryContainer
        InsightSeverity.LOW -> MaterialTheme.colorScheme.onTertiaryContainer
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Shapes.medium,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isAnomaly) Icons.Default.Warning else Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = insight.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = insight.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor
            )

            if (insight.actionableTip.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Card(
                    shape = Shapes.extraSmall,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "💡 Tip: ${insight.actionableTip}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
