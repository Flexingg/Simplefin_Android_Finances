package com.randallengineering.finances.ui.screens.ai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.randallengineering.finances.core.ai.ProposedCategorizationDto
import com.randallengineering.finances.core.ai.ToolExecutionResult
import com.randallengineering.finances.core.theme.FinanceGreen
import com.randallengineering.finances.core.theme.FinanceGreenDark
import com.randallengineering.finances.core.theme.Shapes
import com.randallengineering.finances.core.util.CurrencyFormatter
import com.randallengineering.finances.data.repository.AiProviderMode
import com.randallengineering.finances.domain.usecase.ChatMessage
import com.randallengineering.finances.domain.usecase.MessageSender
import com.randallengineering.finances.ui.components.ExpressiveCard
import com.randallengineering.finances.ui.components.SnapshotExportSheet
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAdvisorScreen(
    viewModel: AiAdvisorViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    // Scroll to bottom on new message
    LaunchedEffect(uiState.messages.size, uiState.isProcessing) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    if (uiState.isSnapshotSheetOpen && uiState.snapshot != null) {
        SnapshotExportSheet(
            snapshot = uiState.snapshot!!,
            onDismiss = { viewModel.closeSnapshotSheet() }
        )
    }

    if (uiState.showApiKeyDialog) {
        GeminiApiKeyModal(
            currentKey = uiState.apiKeyInput,
            currentMode = uiState.providerMode,
            onDismiss = { viewModel.closeApiKeyDialog() },
            onSave = { key, mode -> viewModel.saveApiKey(key, mode) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(FinanceGreen.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = FinanceGreen, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("AI Financial Advisor", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                            // Gemini Status Pill
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { viewModel.openApiKeyDialog() }
                                    .background(if (uiState.isApiKeyConfigured) FinanceGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (uiState.isApiKeyConfigured) FinanceGreen else Color(0xFFE5A500))
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = if (uiState.isApiKeyConfigured) "Gemini 2.5 Flash (Active)" else "Built-in / Set Key",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.isApiKeyConfigured) FinanceGreenDark else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.openSnapshotSheet() }) {
                        Icon(Icons.Default.DataObject, contentDescription = "Export Snapshot", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { viewModel.clearChat() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear Chat", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .imePadding()
                .navigationBarsPadding()
        ) {
            // Chat Message Stream
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(uiState.messages, key = { it.id }) { message ->
                    ChatMessageItem(
                        message = message,
                        onApplyProposedBatch = { batch ->
                            viewModel.sendMessage("apply proposed categorizations")
                        },
                        onActionClick = { actionText ->
                            viewModel.sendMessage(actionText)
                        }
                    )
                }

                if (uiState.isProcessing) {
                    item {
                        ThinkingIndicatorBubble()
                    }
                }
            }

            // Quick Prompt Suggestion Pills
            val suggestedActions = uiState.messages.lastOrNull()?.suggestedActions
                ?: listOf("ðŸ–ï¸ Simulate retirement at age 62", "ðŸ’µ Daily safe allowance", "ðŸ·ï¸ Review 10 transactions", "ðŸ’³ Snowball vs Avalanche")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                suggestedActions.forEach { action ->
                    SuggestionChip(
                        onClick = { viewModel.sendMessage(action) },
                        label = { Text(action, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        shape = RoundedCornerShape(16.dp),
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            // Input Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.inputText,
                    onValueChange = { viewModel.onInputTextChange(it) },
                    placeholder = { Text("Ask Gemini or run MCP tool...", fontSize = 13.sp) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { viewModel.sendMessage() }),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FinanceGreen,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                FilledIconButton(
                    onClick = { viewModel.sendMessage() },
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = FinanceGreen),
                    modifier = Modifier.size(46.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(
    message: ChatMessage,
    onApplyProposedBatch: (List<ProposedCategorizationDto>) -> Unit,
    onActionClick: (String) -> Unit
) {
    val isUser = message.sender == MessageSender.USER

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(if (isUser) 0.85f else 0.95f),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            if (!isUser) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(FinanceGreen.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.SmartToy, contentDescription = null, tint = FinanceGreen, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(8.dp))
            }

            Column(modifier = Modifier.weight(1f, fill = false)) {
                // Speech Bubble
                Box(
                    modifier = Modifier
                        .clip(
                            RoundedCornerShape(
                                topStart = 18.dp,
                                topEnd = 18.dp,
                                bottomStart = if (isUser) 18.dp else 4.dp,
                                bottomEnd = if (isUser) 4.dp else 18.dp
                            )
                        )
                        .background(
                            if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = message.text,
                        color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp
                    )
                }

                // Tool Execution Cards
                val tools = message.executedTools.ifEmpty { listOfNotNull(message.toolResult) }
                for (tool in tools) {
                    Spacer(Modifier.height(8.dp))
                    ToolExecutionCard(tool)
                }

                // Batch Categorization Review Card
                if (message.proposedCategorizations.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    BatchCategorizationReviewCard(
                        proposals = message.proposedCategorizations,
                        onApplyAll = { onApplyProposedBatch(message.proposedCategorizations) }
                    )
                }
            }
        }
    }
}

@Composable
fun ToolExecutionCard(tool: ToolExecutionResult) {
    ExpressiveCard(
        modifier = Modifier.fillMaxWidth(),
        shape = Shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (tool.success) FinanceGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (tool.success) Icons.Default.CheckCircle else Icons.Default.Build,
                contentDescription = null,
                tint = if (tool.success) FinanceGreen else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = "MCP Tool: ${tool.toolName}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = if (tool.success) FinanceGreenDark else MaterialTheme.colorScheme.error
                )
                Text(
                    text = tool.message,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun BatchCategorizationReviewCard(
    proposals: List<ProposedCategorizationDto>,
    onApplyAll: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Proposed Categorizations (${proposals.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Button(
                    onClick = onApplyAll,
                    colors = ButtonDefaults.buttonColors(containerColor = FinanceGreen),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Apply All", color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            proposals.take(5).forEach { prop ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(prop.originalDesc, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1)
                        Text(
                            "${prop.suggestedMain}${if (prop.suggestedSub.isNotBlank()) " > ${prop.suggestedSub}" else ""} â€¢ ${prop.confidenceReason}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        CurrencyFormatter.formatWithSign(prop.amount),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (prop.amount < 0) MaterialTheme.colorScheme.error else FinanceGreen
                    )
                }
            }
            if (proposals.size > 5) {
                Text("+ ${proposals.size - 5} more items...", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
fun ThinkingIndicatorBubble() {
    Row(
        modifier = Modifier.fillMaxWidth(0.6f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(FinanceGreen.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.SmartToy, contentDescription = null, tint = FinanceGreen, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = FinanceGreen)
                Spacer(Modifier.width(8.dp))
                Text("Analyzing ledger & tools...", fontSize = 12.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
            }
        }
    }
}

@Composable
fun GeminiApiKeyModal(
    currentKey: String,
    currentMode: AiProviderMode,
    onDismiss: () -> Unit,
    onSave: (String, AiProviderMode) -> Unit
) {
    var keyInput by remember { mutableStateOf(currentKey) }
    var selectedMode by remember { mutableStateOf(currentMode) }
    var isPasswordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Key, contentDescription = null, tint = FinanceGreen)
                Spacer(Modifier.width(8.dp))
                Text("Gemini AI Configuration", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "You can supply a free Gemini API Key from Google AI Studio, or use the built-in Firebase Vertex AI connection.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedMode == AiProviderMode.CUSTOM_KEY,
                        onClick = { selectedMode = AiProviderMode.CUSTOM_KEY },
                        label = { Text("Gemini API Key") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = FinanceGreen.copy(alpha = 0.2f))
                    )
                    FilterChip(
                        selected = selectedMode == AiProviderMode.BUILTIN_VERTEX,
                        onClick = { selectedMode = AiProviderMode.BUILTIN_VERTEX },
                        label = { Text("Built-in Vertex AI") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = FinanceGreen.copy(alpha = 0.2f))
                    )
                }

                if (selectedMode == AiProviderMode.CUSTOM_KEY) {
                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        label = { Text("Gemini API Key") },
                        placeholder = { Text("AIzaSy...") },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                } else {
                    Text(
                        "Built-in Google / Firebase Vertex AI uses your project credentials automatically. Free tier quotas apply.",
                        style = MaterialTheme.typography.labelSmall,
                        color = FinanceGreenDark
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(keyInput, selectedMode) },
                colors = ButtonDefaults.buttonColors(containerColor = FinanceGreen),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Save & Apply", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

