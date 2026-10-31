package com.sukisu.ultra.ai

import java.util.Locale
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

/** Token counts reported by a provider for one completion. */
data class AiTokenUsage(
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val totalTokens: Int = 0,
) {
    val isEmpty: Boolean
        get() = promptTokens == 0 && completionTokens == 0 && totalTokens == 0

    operator fun plus(other: AiTokenUsage): AiTokenUsage = AiTokenUsage(
        promptTokens = promptTokens + other.promptTokens,
        completionTokens = completionTokens + other.completionTokens,
        totalTokens = totalTokens + other.totalTokens,
    )
}

/** Session counters shown in the chat chip, dialog and settings page. */
data class AiUsageState(
    val session: AiTokenUsage = AiTokenUsage(),
    val last: AiTokenUsage? = null,
    val requests: Int = 0,
)

internal fun formatTokenCount(value: Int): String = String.format(Locale.US, "%,d", value)

/**
 * Reads the first usage shape it recognizes: OpenAI (`prompt_tokens`,
 * `completion_tokens`, `total_tokens`), Anthropic (`input_tokens`,
 * `output_tokens`) or Ollama (`prompt_eval_count`, `eval_count`).
 */
internal fun JsonObject.tokenUsage(): AiTokenUsage? {
    val prompt = intOrZero("prompt_tokens", "input_tokens", "prompt_eval_count")
    val completion = intOrZero("completion_tokens", "output_tokens", "eval_count")
    val total = (this["total_tokens"] as? JsonPrimitive)?.intOrNull ?: (prompt + completion)
    if (prompt == 0 && completion == 0 && total == 0) return null
    return AiTokenUsage(
        promptTokens = prompt,
        completionTokens = completion,
        totalTokens = total,
    )
}

private fun JsonObject.intOrZero(vararg keys: String): Int =
    keys.firstNotNullOfOrNull { key -> (this[key] as? JsonPrimitive)?.intOrNull } ?: 0
