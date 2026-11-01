package com.sukisu.ultra.ai

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private fun AiUsageState.hasUsage(): Boolean = requests > 0 && !session.isEmpty

// ------------------------------------------------------------- chat top bars

@Composable
internal fun AiUsageChip(
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
internal fun AiUsageDialog(
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
                AiDialogHeader(
                    icon = Icons.Rounded.Memory,
                    title = stringResource(R.string.ai_usage_section),
                )
                AiUsagePanel(state = state, onReset = onReset)
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
internal fun AiUsageSection(
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
            AiDialogHeader(
                icon = Icons.Rounded.Memory,
                title = stringResource(R.string.ai_usage_section),
            )
            Text(
                text = stringResource(R.string.ai_usage_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AiUsagePanel(state = state, onReset = onReset)
        }
    }
}

@Composable
private fun AiUsagePanel(
    state: AiUsageState,
    onReset: () -> Unit,
) {
    if (!state.hasUsage()) {
        AiDialogNotice(
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
        UsageCard {
            UsageRow(
                label = stringResource(R.string.ai_usage_prompt),
                value = formatTokenCount(state.session.promptTokens),
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
            )
            UsageRow(
                label = stringResource(R.string.ai_usage_completion),
                value = formatTokenCount(state.session.completionTokens),
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
            )
            UsageRow(
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
            UsageCard {
                UsageRow(
                    label = stringResource(R.string.ai_usage_prompt),
                    value = formatTokenCount(last.promptTokens),
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                )
                UsageRow(
                    label = stringResource(R.string.ai_usage_completion),
                    value = formatTokenCount(last.completionTokens),
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                )
                UsageRow(
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
private fun UsageCard(content: @Composable () -> Unit) {
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
private fun UsageRow(label: String, value: String) {
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
