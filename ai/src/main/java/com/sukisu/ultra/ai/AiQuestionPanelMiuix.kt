package com.sukisu.ultra.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sukisu.ultra.ai.R
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

@Composable
fun AiQuestionPanelMiuix(
    question: AiUserQuestion,
    onAnswer: (String?) -> Unit,
) {
    var custom by remember(question.question, question.options) { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                colorScheme.surfaceContainerHigh,
                RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            )
            .heightIn(max = 420.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AiIconBadgeMiuix(
                icon = Icons.AutoMirrored.Rounded.HelpOutline,
                size = 34.dp,
                iconSize = 18.dp,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(
                    text = stringResource(R.string.ai_question_title),
                    style = MiuixTheme.textStyles.title4,
                    color = colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.ai_status_waiting_choice),
                    style = MiuixTheme.textStyles.body2,
                    color = colorScheme.onSurfaceVariantSummary,
                )
            }
            IconButton(
                onClick = { onAnswer(null) },
                modifier = Modifier.size(30.dp),
            ) {
                Icon(
                    imageVector = MiuixIcons.Close,
                    contentDescription = stringResource(R.string.ai_question_dismiss),
                    modifier = Modifier.size(16.dp),
                    tint = colorScheme.onSurfaceVariantSummary,
                )
            }
        }
        Text(
            text = question.question,
            style = MiuixTheme.textStyles.body1,
            color = colorScheme.onSurface,
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            question.options.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colorScheme.surface)
                        .clickable { onAnswer(option) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = option,
                        modifier = Modifier.weight(1f),
                        style = MiuixTheme.textStyles.body1,
                        color = colorScheme.onSurface,
                    )
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
        }
        if (question.allowCustom) {
            Row(verticalAlignment = Alignment.Bottom) {
                TextField(
                    value = custom,
                    onValueChange = { custom = it },
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.ai_question_custom_hint),
                    useLabelAsPlaceholder = true,
                    maxLines = 3,
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        val answer = custom.trim()
                        if (answer.isNotEmpty()) onAnswer(answer)
                    },
                    enabled = custom.isNotBlank(),
                    modifier = Modifier.size(44.dp),
                    backgroundColor = if (custom.isNotBlank()) {
                        colorScheme.primary
                    } else {
                        colorScheme.surfaceContainerHighest
                    },
                    cornerRadius = 14.dp,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = stringResource(R.string.ai_question_send),
                        modifier = Modifier.size(20.dp),
                        tint = if (custom.isNotBlank()) {
                            colorScheme.onPrimary
                        } else {
                            colorScheme.onSurfaceVariantSummary
                        },
                    )
                }
            }
        }
    }
}
