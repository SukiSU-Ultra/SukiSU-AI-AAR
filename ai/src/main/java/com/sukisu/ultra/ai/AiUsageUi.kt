package com.sukisu.ultra.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog as MiuixOverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

private fun AiUsageState.hasUsage(): Boolean = requests > 0 && !session.isEmpty

// ------------------------------------------------------------- chat top bars

@Composable
internal fun AiUsageChipMaterial(
    state: AiUsageState,
    onClick: () -> Unit,
) {
    if (!state.hasUsage()) return
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(end = 4.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Memory,
                contentDescription = stringResource(R.string.ai_usage),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = formatTokenCount(state.session.totalTokens),
                modifier = Modifier.widthIn(max = 104.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
internal fun AiUsageChipMiuix(
    state: AiUsageState,
    onClick: () -> Unit,
) {
    if (!state.hasUsage()) return
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        MiuixIcon(
            imageVector = Icons.Rounded.Memory,
            contentDescription = stringResource(R.string.ai_usage),
            tint = colorScheme.primary,
            modifier = Modifier.size(16.dp),
        )
        MiuixText(
            text = formatTokenCount(state.session.totalTokens),
            modifier = Modifier.widthIn(max = 112.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MiuixTheme.textStyles.body2,
        )
    }
}

// ------------------------------------------------------------------ dialogs

@Composable
internal fun AiUsageDialogMaterial(
    state: AiUsageState,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    AiExpressiveDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                AiDialogHeaderMaterial(
                    icon = Icons.Rounded.Memory,
                    title = stringResource(R.string.ai_usage_section),
                )
                AiUsagePanelMaterial(state = state, onReset = onReset)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ai_close))
            }
        },
    )
}

@Composable
internal fun AiUsageDialogMiuix(
    state: AiUsageState,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    MiuixOverlayDialog(
        show = true,
        title = null,
        onDismissRequest = onDismiss,
        content = {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                AiDialogHeaderMiuix(
                    icon = Icons.Rounded.Memory,
                    title = stringResource(R.string.ai_usage_section),
                )
                AiUsagePanelMiuix(state = state, onReset = onReset)
                MiuixTextButton(
                    text = stringResource(R.string.ai_close),
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    )
}

// ---------------------------------------------------------------- settings

@Composable
internal fun AiUsageSectionMaterial(
    state: AiUsageState,
    onReset: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AiDialogHeaderMaterial(
                icon = Icons.Rounded.Memory,
                title = stringResource(R.string.ai_usage_section),
            )
            Text(
                text = stringResource(R.string.ai_usage_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AiUsagePanelMaterial(state = state, onReset = onReset)
        }
    }
}

@Composable
internal fun AiUsageSectionMiuix(
    state: AiUsageState,
    onReset: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AiDialogHeaderMiuix(
            icon = Icons.Rounded.Memory,
            title = stringResource(R.string.ai_usage_section),
        )
        MiuixText(
            text = stringResource(R.string.ai_usage_hint),
            style = MiuixTheme.textStyles.body2,
            color = colorScheme.onSurfaceVariantSummary,
        )
        AiUsagePanelMiuix(state = state, onReset = onReset)
    }
}

// ------------------------------------------------------------ shared bodies

@Composable
private fun AiUsagePanelMaterial(
    state: AiUsageState,
    onReset: () -> Unit,
) {
    if (!state.hasUsage()) {
        AiDialogNoticeMaterial(
            text = stringResource(R.string.ai_usage_empty),
            icon = Icons.Rounded.Memory,
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.ai_usage_session),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatTokenCount(state.session.totalTokens),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        UsageCardMaterial {
            UsageRowMaterial(
                label = stringResource(R.string.ai_usage_prompt),
                value = formatTokenCount(state.session.promptTokens),
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
            )
            UsageRowMaterial(
                label = stringResource(R.string.ai_usage_completion),
                value = formatTokenCount(state.session.completionTokens),
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
            )
            UsageRowMaterial(
                label = stringResource(R.string.ai_usage_requests_label),
                value = stringResource(R.string.ai_usage_requests, state.requests),
            )
        }
        val last = state.last
        if (last != null) {
            Text(
                text = stringResource(R.string.ai_usage_last),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            UsageCardMaterial {
                UsageRowMaterial(
                    label = stringResource(R.string.ai_usage_prompt),
                    value = formatTokenCount(last.promptTokens),
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                )
                UsageRowMaterial(
                    label = stringResource(R.string.ai_usage_completion),
                    value = formatTokenCount(last.completionTokens),
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                )
                UsageRowMaterial(
                    label = stringResource(R.string.ai_usage_total),
                    value = formatTokenCount(last.totalTokens),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onReset) {
                Text(stringResource(R.string.ai_usage_reset))
            }
        }
    }
}

@Composable
private fun AiUsagePanelMiuix(
    state: AiUsageState,
    onReset: () -> Unit,
) {
    if (!state.hasUsage()) {
        AiDialogNoticeMiuix(
            text = stringResource(R.string.ai_usage_empty),
            icon = Icons.Rounded.Memory,
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            MiuixText(
                text = stringResource(R.string.ai_usage_session),
                style = MiuixTheme.textStyles.body2,
                color = colorScheme.onSurfaceVariantSummary,
            )
            MiuixText(
                text = formatTokenCount(state.session.totalTokens),
                style = MiuixTheme.textStyles.title1,
                color = colorScheme.primary,
            )
        }
        UsageCardMiuix {
            UsageRowMiuix(
                label = stringResource(R.string.ai_usage_prompt),
                value = formatTokenCount(state.session.promptTokens),
            )
            AiDialogDividerMiuix()
            UsageRowMiuix(
                label = stringResource(R.string.ai_usage_completion),
                value = formatTokenCount(state.session.completionTokens),
            )
            AiDialogDividerMiuix()
            UsageRowMiuix(
                label = stringResource(R.string.ai_usage_requests_label),
                value = stringResource(R.string.ai_usage_requests, state.requests),
            )
        }
        val last = state.last
        if (last != null) {
            MiuixText(
                text = stringResource(R.string.ai_usage_last),
                style = MiuixTheme.textStyles.body2,
                color = colorScheme.onSurfaceVariantSummary,
            )
            UsageCardMiuix {
                UsageRowMiuix(
                    label = stringResource(R.string.ai_usage_prompt),
                    value = formatTokenCount(last.promptTokens),
                )
                AiDialogDividerMiuix()
                UsageRowMiuix(
                    label = stringResource(R.string.ai_usage_completion),
                    value = formatTokenCount(last.completionTokens),
                )
                AiDialogDividerMiuix()
                UsageRowMiuix(
                    label = stringResource(R.string.ai_usage_total),
                    value = formatTokenCount(last.totalTokens),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            MiuixTextButton(
                text = stringResource(R.string.ai_usage_reset),
                onClick = onReset,
            )
        }
    }
}

@Composable
private fun UsageCardMaterial(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun UsageRowMaterial(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (value.isNotEmpty()) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun UsageCardMiuix(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colorScheme.surfaceContainerHigh)
            .padding(horizontal = 14.dp, vertical = 4.dp),
    ) {
        content()
    }
}

@Composable
private fun UsageRowMiuix(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MiuixText(
            text = label,
            style = MiuixTheme.textStyles.body2,
            color = colorScheme.onSurfaceVariantSummary,
        )
        if (value.isNotEmpty()) {
            MiuixText(
                text = value,
                style = MiuixTheme.textStyles.body2,
                color = colorScheme.onSurface,
            )
        }
    }
}
