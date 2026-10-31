package com.sukisu.ultra.ai
import com.sukisu.ultra.ai.api.AiUiMode

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sukisu.ultra.ai.R
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun AiSettingsScreen(uiMode: AiUiMode, onBack: () -> Unit) {
    val model = rememberAiChatViewModel()
    val state by model.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val localNetworkPermissionDenied = stringResource(R.string.ai_local_network_permission_denied)
    val scope = rememberCoroutineScope()
    var draft by remember(state.config) { mutableStateOf(state.config) }
    var testState by remember { mutableStateOf(AiTestState()) }
    var fetchingModels by remember { mutableStateOf(false) }
    var modelOptions by remember { mutableStateOf<List<String>>(emptyList()) }
    var modelTarget by remember { mutableStateOf<Boolean?>(null) }
    var pendingNetworkAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val networkPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pendingNetworkAction?.invoke()
        } else {
            testState = AiTestState(
                message = localNetworkPermissionDenied,
                success = false,
            )
            Toast.makeText(context, localNetworkPermissionDenied, Toast.LENGTH_LONG).show()
        }
        pendingNetworkAction = null
    }
    val runWithLocalNetwork: (() -> Unit) -> Unit = { action ->
        if (AiLocalNetwork.needsPermission(context, draft)) {
            pendingNetworkAction = action
            networkPermissionLauncher.launch(AiLocalNetwork.PERMISSION)
        } else {
            action()
        }
    }

    fun advancedOptionsError(config: AiConfig = draft): Throwable? =
        if (config.advancedMode && config.advancedOptionsJson.isNotBlank()) {
            AiAdvancedOptions.parse(config.advancedOptionsJson).exceptionOrNull()
        } else {
            null
        }

    fun reportAdvancedOptionsError(error: Throwable) {
        Toast.makeText(
            context,
            context.getString(
                R.string.ai_advanced_options_invalid,
                error.message ?: error.javaClass.simpleName,
            ),
            Toast.LENGTH_LONG,
        ).show()
    }

    fun saveDraft(showConfirmation: Boolean) {
        advancedOptionsError()?.let {
            reportAdvancedOptionsError(it)
            return
        }
        model.updateConfig(draft)
        if (showConfirmation) {
            Toast.makeText(context, R.string.ai_saved, Toast.LENGTH_SHORT).show()
        }
    }

    val handleBack: () -> Unit = {
        saveDraft(showConfirmation = false)
        onBack()
    }
    BackHandler(onBack = handleBack)

    val fetchModels: (Boolean) -> Unit = fetch@ { forImage ->
        advancedOptionsError()?.let {
            reportAdvancedOptionsError(it)
            return@fetch
        }
        if (!fetchingModels) {
            if (draft.endpoint.isBlank()) {
                Toast.makeText(context, R.string.ai_address_required, Toast.LENGTH_SHORT).show()
            } else {
                runWithLocalNetwork {
                    fetchingModels = true
                    scope.launch {
                        try {
                            val models = AiApiClient.fetchModels(draft)
                            if (models.isEmpty()) {
                                Toast.makeText(context, R.string.ai_no_models, Toast.LENGTH_SHORT).show()
                            } else {
                                modelOptions = models
                                modelTarget = forImage
                            }
                        } catch (cancelled: kotlinx.coroutines.CancellationException) {
                            throw cancelled
                        } catch (error: Exception) {
                            Toast.makeText(
                                context,
                                error.message ?: context.getString(R.string.ai_fetch_models_failed),
                                Toast.LENGTH_LONG,
                            ).show()
                        } finally {
                            fetchingModels = false
                        }
                    }
                }
            }
        }
    }

    val testConnection: () -> Unit = test@{
        advancedOptionsError()?.let {
            reportAdvancedOptionsError(it)
            return@test
        }
        if (!testState.testing) {
            runWithLocalNetwork {
                testState = AiTestState(testing = true)
                scope.launch { testState = runAiConnectionTest(draft) }
            }
        }
    }

    val updateDraft: (AiConfig) -> Unit = { updated ->
        draft = updated
        // Advanced JSON is expert input; keep the last valid stored config
        // while the editor text is invalid instead of persisting a broken one.
        if (advancedOptionsError(updated) == null) {
            model.updateConfig(updated)
        }
    }

    when (uiMode) {
        AiUiMode.Material -> AiSettingsMaterial(
            draft = draft,
            onDraftChange = updateDraft,
            testState = testState,
            fetchingModels = fetchingModels,
            usageState = state.usage,
            onResetUsage = model::resetUsage,
            onSave = { saveDraft(showConfirmation = true) },
            onTest = testConnection,
            onFetchModels = fetchModels,
            onBack = handleBack,
        )

        AiUiMode.Miuix -> AiSettingsMiuix(
            draft = draft,
            onDraftChange = updateDraft,
            testState = testState,
            fetchingModels = fetchingModels,
            usageState = state.usage,
            onResetUsage = model::resetUsage,
            onSave = { saveDraft(showConfirmation = true) },
            onTest = testConnection,
            onFetchModels = fetchModels,
            onBack = handleBack,
        )
    }

    val target = modelTarget
    if (target != null) {
        val onSelect: (String) -> Unit = { selected ->
            draft = if (target) draft.copy(imageModel = selected) else draft.copy(model = selected)
            modelTarget = null
        }
        val dismiss = { modelTarget = null }
        val selectedModel = if (target) draft.imageModel else draft.model
        when (uiMode) {
            AiUiMode.Material ->
                AiModelListDialogMaterial(modelOptions, selectedModel, onSelect, dismiss)
            AiUiMode.Miuix ->
                AiModelListDialogMiuix(modelOptions, selectedModel, onSelect, dismiss)
        }
    }
}

@Composable
private fun AiSettingsMaterial(
    draft: AiConfig,
    onDraftChange: (AiConfig) -> Unit,
    testState: AiTestState,
    fetchingModels: Boolean,
    usageState: AiUsageState,
    onResetUsage: () -> Unit,
    onSave: () -> Unit,
    onTest: () -> Unit,
    onFetchModels: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    AiExpressiveScaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.ai_settings)) },
                navigationIcon = { AiTopBarBackButton(onClick = onBack) },
                colors = aiExpressiveTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                scrollBehavior = scroll,
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .nestedScroll(scroll.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .navigationBarsPadding(),
        ) {
            if (AiLocalNetwork.isSupported() && AiLocalNetwork.isLanEndpoint(draft)) {
                Text(
                    text = stringResource(R.string.ai_local_network_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                )
            }
            AiConfigEditorMaterial(
                draft = draft,
                onDraftChange = onDraftChange,
                testState = testState,
                onTest = onTest,
                onSave = onSave,
                fetchingModels = fetchingModels,
                onFetchModels = onFetchModels,
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )
            AiUsageSectionMaterial(
                state = usageState,
                onReset = onResetUsage,
            )
        }
    }
}

@Composable
private fun AiSettingsMiuix(
    draft: AiConfig,
    onDraftChange: (AiConfig) -> Unit,
    testState: AiTestState,
    fetchingModels: Boolean,
    usageState: AiUsageState,
    onResetUsage: () -> Unit,
    onSave: () -> Unit,
    onTest: () -> Unit,
    onFetchModels: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val scroll = MiuixScrollBehavior()
    AiAppScaffold(
        containerColor = colorScheme.surface,
        topBar = {
            TopAppBar(
                title = stringResource(R.string.ai_settings),
                color = colorScheme.surface,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        MiuixIcon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = null,
                            tint = colorScheme.onBackground,
                        )
                    }
                },
                scrollBehavior = scroll,
            )
        },
        popupHost = { },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.surface)
                .padding(padding)
                .nestedScroll(scroll.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .navigationBarsPadding(),
        ) {
            if (AiLocalNetwork.isSupported() && AiLocalNetwork.isLanEndpoint(draft)) {
                MiuixText(
                    text = stringResource(R.string.ai_local_network_hint),
                    style = MiuixTheme.textStyles.body2,
                    color = colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                )
            }
            AiAppCard(modifier = Modifier.fillMaxWidth()) {
                AiConfigEditorMiuix(
                    draft = draft,
                    onDraftChange = onDraftChange,
                    testState = testState,
                    onTest = onTest,
                    onSave = onSave,
                    fetchingModels = fetchingModels,
                    onFetchModels = onFetchModels,
                )
            }
            AiAppCard(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) {
                AiUsageSectionMiuix(
                    state = usageState,
                    onReset = onResetUsage,
                )
            }
        }
    }
}

@Composable
private fun AiModelListDialogMaterial(
    models: List<String>,
    selected: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AiExpressiveDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AiDialogHeaderMaterial(
                    icon = Icons.Rounded.Tune,
                    title = stringResource(R.string.ai_select_model),
                )
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(models) { model ->
                        val isSelected = model == selected
                        Surface(
                            onClick = { onSelect(model) },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            contentColor = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = model,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
    )
}

@Composable
private fun AiModelListDialogMiuix(
    models: List<String>,
    selected: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    OverlayDialog(
        show = true,
        title = null,
        onDismissRequest = onDismiss,
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AiDialogHeaderMiuix(
                    icon = Icons.Rounded.Tune,
                    title = stringResource(R.string.ai_select_model),
                )
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(models) { model ->
                        val isSelected = model == selected
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) {
                                        colorScheme.primaryContainer
                                    } else {
                                        colorScheme.surfaceContainerHigh
                                    }
                                )
                                .clickable { onSelect(model) }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            MiuixText(
                                text = model,
                                modifier = Modifier.weight(1f),
                                style = MiuixTheme.textStyles.body1,
                                color = if (isSelected) {
                                    colorScheme.onPrimaryContainer
                                } else {
                                    colorScheme.onSurface
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (isSelected) {
                                MiuixIcon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
                MiuixTextButton(
                    text = stringResource(android.R.string.cancel),
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    )
}
