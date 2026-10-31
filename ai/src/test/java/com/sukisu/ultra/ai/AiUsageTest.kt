package com.sukisu.ultra.ai

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AiUsageTest {

    private fun obj(body: String) = Json.parseToJsonElement(body.trimIndent()).jsonObject

    @Test
    fun openAiUsageShapeIsRead() {
        val usage = obj(
            """{"prompt_tokens":100,"completion_tokens":40,"total_tokens":140}"""
        ).tokenUsage()
        assertEquals(100, usage?.promptTokens)
        assertEquals(40, usage?.completionTokens)
        assertEquals(140, usage?.totalTokens)
    }

    @Test
    fun anthropicAndOllamaShapesAreRead() {
        val anthropic = obj("""{"input_tokens":9,"output_tokens":3}""").tokenUsage()
        assertEquals(9, anthropic?.promptTokens)
        assertEquals(3, anthropic?.completionTokens)
        assertEquals(12, anthropic?.totalTokens)

        val ollama = obj("""{"prompt_eval_count":7,"eval_count":5}""").tokenUsage()
        assertEquals(7, ollama?.promptTokens)
        assertEquals(5, ollama?.completionTokens)
        assertEquals(12, ollama?.totalTokens)
    }

    @Test
    fun missingUsageIsNull() {
        assertNull(obj("""{"id":"x"}""").tokenUsage())
        assertNull(obj("""{"prompt_tokens":0,"completion_tokens":0}""").tokenUsage())
    }

    @Test
    fun countersAccumulate() {
        val first = AiTokenUsage(10, 5, 15)
        val second = AiTokenUsage(20, 10, 30)
        val total = first + second
        assertEquals(30, total.promptTokens)
        assertEquals(15, total.completionTokens)
        assertEquals(45, total.totalTokens)
    }

    @Test
    fun countsAreGroupedForDisplay() {
        assertEquals("0", formatTokenCount(0))
        assertEquals("1,234", formatTokenCount(1234))
        assertEquals("1,234,567", formatTokenCount(1_234_567))
    }
}
