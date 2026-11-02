package com.sukisu.ultra.ai

import com.sukisu.ultra.ai.api.AiNavigationTarget

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AddComment
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SmallExtendedFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.pointerInput
import coil.compose.AsyncImage
import com.sukisu.ultra.ai.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun AiChat(
    state: AiChatUiState,
    actions: AiChatActions,
) {
    val listState = rememberLazyListState()
    BackHandler(enabled = state.pendingQuestion != null) {
        actions.onAnswerQuestion(null)
    }
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(state.pendingQuestion) {
        if (state.pendingQuestion != null) keyboardController?.hide()
    }
    val clearDialog = rememberAiConfirmDialog(onConfirm = actions.onClearHistory)
    val clearTitle = stringResource(R.string.ai_clear_history_title)
    val clearContent = stringResource(R.string.ai_clear_history_message)
    val clearConfirm = stringResource(R.string.ai_clear_history_confirm)
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }
    var showUsage by remember { mutableStateOf(false) }
    val deleteTitle = stringResource(R.string.ai_delete_message_title)
    val deleteContent = stringResource(R.string.ai_delete_message_message)
    val deleteConfirm = stringResource(R.string.ai_delete_message_confirm)
    val deleteDialog = rememberAiConfirmDialog(
        onConfirm = {
            pendingDeleteId?.let(actions.onDeleteMessage)
            pendingDeleteId = null
        },
        onDismiss = { pendingDeleteId = null },
    )

    // Follow new content only while the user is already at the bottom. Browsing
    // older messages must not be interrupted by the streaming answer or by a
    // refresh of the follow-up chips.
    val shouldFollow = remember { mutableStateOf(true) }
    val userScrolling = remember { mutableStateOf(false) }
    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            val lastItem = info.totalItemsCount - 1
            val atLastItem = lastItem >= 0 && lastVisible >= lastItem
            atLastItem && listState.canScrollForward.not()
        }.collect { atBottom ->
            // Ignore the frames produced by our own scroll while a drag is in progress.
            if (!userScrolling.value) shouldFollow.value = atBottom
        }
    }
    LaunchedEffect(
        state.messages.size,
        state.messages.lastOrNull()?.content?.length,
        state.suggestions,
        state.suggestionsLoading,
        state.isSending,
    ) {
        if (state.messages.isEmpty() || !shouldFollow.value || userScrolling.value) return@LaunchedEffect
        val last = listState.layoutInfo.totalItemsCount - 1
        if (last >= 0) listState.scrollToItem(last)
    }

    // The list opens on the newest message. The jump button then points back to
    // the bottom, or to the top once the user has already scrolled down there.
    val scope = rememberCoroutineScope()
    val atBottom by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            info.totalItemsCount == 0 || (last >= info.totalItemsCount - 1 && !listState.canScrollForward)
        }
    }
    LaunchedEffect(state.messages.isNotEmpty()) {
        val last = listState.layoutInfo.totalItemsCount - 1
        if (last >= 0) listState.scrollToItem(last)
    }
    val jumpToBottom = {
        scope.launch {
            userScrolling.value = false
            shouldFollow.value = true
            val last = listState.layoutInfo.totalItemsCount - 1
            if (last >= 0) listState.animateScrollToItem(last)
        }
    }
    val jumpToTop = {
        scope.launch {
            userScrolling.value = false
            shouldFollow.value = false
            listState.animateScrollToItem(0)
        }
    }

    AiExpressiveScaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ai_chat_title)) },
                navigationIcon = { AiTopBarBackButton(onClick = actions.onBack) },
                actions = {
                    AiUsageChip(
                        state = state.usage,
                        onClick = { showUsage = true },
                    )
                    IconButton(onClick = actions.onOpenSettings) {
                        Icon(Icons.Rounded.Tune, contentDescription = stringResource(R.string.ai_settings))
                    }
                    IconButton(onClick = {
                        clearDialog.showConfirm(
                            title = clearTitle,
                            content = clearContent,
                            confirm = clearConfirm,
                        )
                    }) {
                        Icon(Icons.Rounded.AddComment, contentDescription = stringResource(R.string.ai_new_chat))
                    }
                },
                colors = aiExpressiveTopAppBarColors(),
                windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Box(modifier = Modifier.weight(1f)) {
                val listModifier = Modifier
                    .fillMaxSize()
                    // A drag means the user is reading; stop the automatic follow until
                    // they scroll back to the bottom. Flinging does not count: letting go
                    // near the bottom should resume following.
                    .pointerInput(listState) {
                        detectDragGestures(
                            onDragStart = { userScrolling.value = true },
                            // Only move events are consumed; start/end stay unconsumed so the
                            // list can still cancel its own fling when the pointer goes down.
                            onDrag = { change, _ -> change.consume() },
                            onDragEnd = {
                                userScrolling.value = false
                                shouldFollow.value = !listState.canScrollForward
                            },
                            onDragCancel = {
                                userScrolling.value = false
                                shouldFollow.value = !listState.canScrollForward
                            },
                        )
                    }
                LazyColumn(
                    state = listState,
                    modifier = listModifier,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (!state.config.isConfigured || !state.config.enabled) {
                        item(key = "setup") {
                            AiSetupCard(
                                enabled = state.config.enabled,
                                onOpenSettings = actions.onOpenSettings,
                            )
                        }
                    }
                    if (state.messages.isEmpty()) {
                        item(key = "welcome") {
                            AiWelcome(
                                suggestions = state.suggestions,
                                loading = state.suggestionsLoading,
                                onSuggestion = { text ->
                                    actions.onInputChange(text)
                                    actions.onSend()
                                },
                                onRefresh = actions.onRefreshSuggestions,
                            )
                        }
                    }
                    items(state.messages, key = { it.id }) { message ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {},
                                    onLongClick = {
                                        if (!message.streaming) {
                                            pendingDeleteId = message.id
                                            deleteDialog.showConfirm(
                                                title = deleteTitle,
                                                content = deleteContent,
                                                confirm = deleteConfirm,
                                            )
                                        }
                                    },
                                )
                        ) {
                            AiMessageBubble(
                                message = message,
                                config = state.config,
                                rootGrantedForRequest = state.rootGrantedForRequest,
                                onNavigate = actions.onNavigate,
                                onCapabilityChange = actions.onSetCapability,
                                onGrantRoot = actions.onGrantRoot,
                                onRunCode = actions.onRunCode,
                            )
                        }
                    }
                    if (state.messages.isNotEmpty() && !state.isSending) {
                        item(key = "followups") {
                            AiFollowUpSuggestions(
                                suggestions = state.suggestions,
                                loading = state.suggestionsLoading,
                                onSuggestion = { text ->
                                    actions.onInputChange(text)
                                    actions.onSend()
                                },
                                onRefresh = actions.onRefreshSuggestions,
                            )
                        }
                    }
                    item(key = "bottom") { Spacer(Modifier.height(4.dp)) }
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = state.messages.isNotEmpty(),
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 12.dp, bottom = 12.dp)
                        // Tall bubbles fill the whole pointer area; the scrim gives the
                        // jump control its shape and keeps it readable over the text.
                        .shadow(3.dp, CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
                ) {
                    SmallExtendedFloatingActionButton(
                        onClick = { if (atBottom) jumpToTop() else jumpToBottom() },
                        icon = {
                            Icon(
                                imageVector = if (atBottom) {
                                    Icons.Rounded.KeyboardArrowUp
                                } else {
                                    Icons.Rounded.KeyboardArrowDown
                                },
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        text = {
                            Text(
                                stringResource(
                                    if (atBottom) R.string.ai_scroll_to_top else R.string.ai_scroll_to_bottom
                                ),
                                maxLines = 1,
                            )
                        },
                    )
                }
            }

            state.pendingQuestion?.let { question ->
                AiQuestionPanel(
                    question = question,
                    onAnswer = actions.onAnswerQuestion,
                )
            }

            AiComposer(
                value = state.input,
                isSending = state.isSending,
                error = state.error,
                onValueChange = actions.onInputChange,
                onSend = actions.onSend,
                onStop = actions.onStop,
                onDismissError = actions.onDismissError,
            )
        }
    }

    state.pendingApproval?.let { pending ->
        AiApprovalDialog(
            pending = pending,
            onAllow = actions.onApprove,
            onAllowAlways = actions.onApproveAlways,
            onDeny = actions.onDeny,
        )
    }

    if (showUsage) {
        AiUsageDialog(
            state = state.usage,
            onReset = actions.onResetUsage,
            onDismiss = { showUsage = false },
        )
    }
}

@Composable
private fun AiFollowUpSuggestions(
    suggestions: List<String>,
    loading: Boolean,
    onSuggestion: (String) -> Unit,
    onRefresh: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.ai_follow_up_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.size(8.dp))
            }
            IconButton(
                onClick = onRefresh,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = stringResource(R.string.ai_suggestions_refresh),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            suggestions.forEach { suggestion ->
                SuggestionChip(
                    onClick = { onSuggestion(suggestion) },
                    label = {
                        Text(
                            text = suggestion,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun AiSetupCard(
    enabled: Boolean,
    onOpenSettings: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AiIconBadge(
                    icon = Icons.Rounded.Tune,
                    container = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.12f),
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    size = 34.dp,
                    iconSize = 18.dp,
                )
                Text(
                    text = stringResource(
                        if (enabled) R.string.ai_setup_required else R.string.ai_setup_disabled
                    ),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.ai_open_settings))
            }
        }
    }
}

@Composable
private fun AiMessageBubble(
    message: AiChatMessage,
    config: AiConfig,
    rootGrantedForRequest: Boolean,
    onNavigate: (AiNavigationTarget) -> Unit,
    onCapabilityChange: (AiCapability, Boolean) -> Unit,
    onGrantRoot: () -> Unit,
    onRunCode: (String) -> Unit,
) {
    val copy = rememberAiCopyText()
    var pendingRiskCapability by remember { mutableStateOf<AiCapability?>(null) }
    var pendingRootGrant by remember { mutableStateOf(false) }
    when (message.role) {
        AiMessageRole.USER -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Surface(
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 24.dp, bottomEnd = 8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Text(
                        text = message.content,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }

        AiMessageRole.ASSISTANT -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.Top,
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(end = 8.dp),
                ) {
                    AiAvatarImage(
                        modifier = Modifier.padding(6.dp),
                        size = 22.dp,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 24.dp, bottomStart = 24.dp, bottomEnd = 24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.weight(1f, fill = false),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AiMarkdown(
                            content = message.content,
                            allowCodeExecution = config.allowCodeExecution,
                            onRunCode = onRunCode,
                        )
                        if (message.streaming) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                            )
                        } else if (message.content.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                IconButton(
                                    onClick = { copy(message.content) },
                                    modifier = Modifier.size(28.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ContentCopy,
                                        contentDescription = stringResource(R.string.ai_copy),
                                        modifier = Modifier.size(15.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        AiMessageRole.TOOL -> {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Terminal,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Text(
                            text = message.toolName ?: stringResource(R.string.ai_tool),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        if (message.streaming) {
                            CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                        }
                        Spacer(Modifier.weight(1f))
                        if (message.content.isNotBlank()) {
                            IconButton(
                                onClick = { copy(message.content) },
                                modifier = Modifier.size(28.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ContentCopy,
                                    contentDescription = stringResource(R.string.ai_copy),
                                    modifier = Modifier.size(15.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            }
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                    ) {
                        SelectionContainer {
                            Text(
                                text = linkifyAiText(
                                    message.content,
                                    MaterialTheme.colorScheme.primary,
                                ),
                                modifier = Modifier
                                    .heightIn(max = 260.dp)
                                    .verticalScroll(rememberScrollState())
                                    .padding(10.dp),
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                    val imagePath = message.imagePath
                    if (imagePath != null) {
                        AsyncImage(
                            model = File(imagePath),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                                .clip(RoundedCornerShape(12.dp)),
                        )
                    }
                    val navigation = message.navigation
                    if (navigation != null) {
                        Button(
                            onClick = { onNavigate(navigation) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = stringResource(
                                    R.string.ai_go_to_page,
                                    message.navigationTitle.orEmpty(),
                                ),
                                modifier = Modifier.padding(start = 6.dp),
                            )
                        }
                    }
                    val capability = message.capability
                    if (capability != null) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    AiIconBadge(
                                        icon = Icons.Rounded.Lock,
                                        container = MaterialTheme.colorScheme.onTertiaryContainer
                                            .copy(alpha = 0.12f),
                                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                        size = 30.dp,
                                        iconSize = 16.dp,
                                    )
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp),
                                    ) {
                                        Text(
                                            text = stringResource(R.string.ai_capability_needed),
                                            style = MaterialTheme.typography.titleSmall,
                                        )
                                        Text(
                                            text = stringResource(R.string.ai_capability_hint),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                                .copy(alpha = 0.8f),
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(
                                            MaterialTheme.colorScheme.onTertiaryContainer
                                                .copy(alpha = 0.12f)
                                        ),
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = stringResource(capability.labelRes()),
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    AiExpressiveSwitch(
                                        checked = config.capabilityEnabled(capability),
                                        onCheckedChange = { enabled ->
                                            if (!enabled) {
                                                onCapabilityChange(capability, false)
                                            } else if (capability.riskKind() != null) {
                                                pendingRiskCapability = capability
                                            } else {
                                                onCapabilityChange(capability, true)
                                            }
                                        },
                                    )
                                }
                                if (capability == AiCapability.ROOT) {
                                    if (rootGrantedForRequest) {
                                        Text(
                                            text = stringResource(R.string.ai_root_granted_for_request),
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    } else if (!config.allowRootAccess) {
                                        FilledTonalButton(
                                            onClick = { pendingRootGrant = true },
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Text(stringResource(R.string.ai_root_allow_once))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        AiMessageRole.ERROR -> {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = message.content,
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }

    pendingRiskCapability?.let { capability ->
        val kind = capability.riskKind()
        if (kind != null) {
            AiRiskDialog(
                kind = kind,
                onConfirm = {
                    pendingRiskCapability = null
                    onCapabilityChange(capability, true)
                },
                onDismiss = { pendingRiskCapability = null },
            )
        }
    }

    if (pendingRootGrant) {
        AiRiskDialog(
            kind = AiRiskKind.ROOT,
            onConfirm = {
                pendingRootGrant = false
                onGrantRoot()
            },
            onDismiss = { pendingRootGrant = false },
        )
    }
}

@Composable
private fun AiWelcome(
    suggestions: List<String>,
    loading: Boolean,
    onSuggestion: (String) -> Unit,
    onRefresh: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            AiAvatarImage(
                modifier = Modifier.padding(12.dp),
                size = 40.dp,
            )
        }
        Text(
            text = stringResource(R.string.ai_welcome_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.ai_welcome_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            suggestions.forEach { suggestion ->
                SuggestionChip(
                    onClick = { onSuggestion(suggestion) },
                    label = {
                        Text(
                            text = suggestion,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                )
            }
        }
        if (loading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                )
                Text(
                    text = stringResource(R.string.ai_suggestions_loading),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        TextButton(onClick = onRefresh) {
            Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = stringResource(R.string.ai_suggestions_refresh),
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
}

@Composable
private fun AiComposer(
    value: String,
    isSending: Boolean,
    error: String?,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onDismissError: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AnimatedVisibility(visible = error != null) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(start = 14.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = error.orEmpty(),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    IconButton(onClick = onDismissError) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.ai_close),
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }
        }
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 2.dp,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(6.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    placeholder = { Text(stringResource(R.string.ai_chat_input_hint)) },
                    maxLines = 5,
                    shape = RoundedCornerShape(22.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        unfocusedBorderColor = Color.Transparent,
                    ),
                    modifier = Modifier.weight(1f),
                )
                FilledIconButton(
                    onClick = if (isSending) onStop else onSend,
                    enabled = isSending || value.isNotBlank(),
                    modifier = Modifier.padding(6.dp).size(48.dp),
                ) {
                    Icon(
                        imageVector = if (isSending) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.Send,
                        contentDescription = stringResource(
                            if (isSending) R.string.ai_stop else R.string.ai_send
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun AiApprovalDialog(
    pending: AiPendingApproval,
    onAllow: () -> Unit,
    onAllowAlways: () -> Unit,
    onDeny: () -> Unit,
) {
    var alwaysAllow by remember(pending.request) { mutableStateOf(false) }
    AiExpressiveDialog(
        onDismissRequest = onDeny,
        title = null,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                AiDialogHeader(
                    icon = Icons.Rounded.Warning,
                    title = stringResource(R.string.ai_approval_title),
                    subtitle = pending.request.title,
                    container = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                AiDialogScrollBox(
                    text = pending.request.detail,
                    maxHeight = 220.dp,
                    monospace = true,
                )
                AiDialogCheckRow(
                    checked = alwaysAllow,
                    onCheckedChange = { alwaysAllow = it },
                    text = stringResource(R.string.ai_approval_always),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (alwaysAllow) onAllowAlways() else onAllow() },
            ) {
                Text(stringResource(R.string.ai_approval_allow))
            }
        },
        dismissButton = {
            TextButton(onClick = onDeny) { Text(stringResource(R.string.ai_approval_deny)) }
        },
    )
}

@Composable
internal fun AiDisclaimerDialog(
    onAccept: () -> Unit,
    onExit: () -> Unit,
) {
    var checked by remember { mutableStateOf(false) }
    AiExpressiveDialog(
        onDismissRequest = onExit,
        title = null,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                AiDialogHeader(
                    icon = Icons.Rounded.Shield,
                    title = stringResource(R.string.ai_disclaimer_title),
                )
                AiDialogScrollBox(
                    text = stringResource(R.string.ai_disclaimer_body),
                    maxHeight = 260.dp,
                )
                AiDialogCheckRow(
                    checked = checked,
                    onCheckedChange = { checked = it },
                    text = stringResource(R.string.ai_disclaimer_check),
                )
            }
        },
        confirmButton = {
            Button(onClick = onAccept, enabled = checked) {
                Text(stringResource(R.string.ai_disclaimer_accept))
            }
        },
        dismissButton = {
            TextButton(onClick = onExit) {
                Text(stringResource(R.string.ai_disclaimer_exit))
            }
        },
    )
}

@Composable
internal fun AiRiskDialog(
    kind: AiRiskKind,
    phrase: String? = null,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    var checked by remember(kind, phrase) { mutableStateOf(false) }
    var phraseInput by remember(kind, phrase) { mutableStateOf("") }
    val phraseMatches = phrase == null || phraseInput.trim().equals(phrase, ignoreCase = true)
    AiExpressiveDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                AiDialogHeader(
                    icon = Icons.Rounded.Warning,
                    title = stringResource(R.string.ai_risk_title),
                    container = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                )
                AiDialogScrollBox(
                    text = stringResource(kind.bodyRes),
                    maxHeight = 240.dp,
                )
                if (phrase != null) {
                    OutlinedTextField(
                        value = phraseInput,
                        onValueChange = { phraseInput = it.take(32) },
                        label = { Text(stringResource(R.string.ai_advanced_phrase)) },
                        placeholder = { Text(stringResource(R.string.ai_advanced_phrase_hint)) },
                        singleLine = true,
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                AiDialogCheckRow(
                    checked = checked,
                    onCheckedChange = { checked = it },
                    text = stringResource(R.string.ai_risk_check),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = checked && phraseMatches,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Text(stringResource(R.string.ai_risk_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ai_risk_cancel))
            }
        },
    )
}
