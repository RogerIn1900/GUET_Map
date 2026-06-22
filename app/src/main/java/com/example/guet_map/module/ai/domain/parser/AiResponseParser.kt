package com.example.guet_map.module.ai.domain.parser

import com.example.guet_map.module.ai.data.model.AiAction
import com.example.guet_map.module.ai.data.model.AiResponse
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DeepSeek 响应解析器：优先解析严格 JSON，失败时安全降级为 chat。
 */
@Singleton
class AiResponseParser @Inject constructor(
    private val gson: Gson
) {

    fun parse(rawContent: String?): AiResponse {
        val content = rawContent.orEmpty().trim()
        if (content.isBlank()) {
            return fallback("AI 暂时没有返回内容，请稍后再试。")
        }

        return runCatching {
            val jsonText = extractFirstJsonObject(content)
            val jsonObject = JsonParser.parseString(jsonText).asJsonObject
            parseJsonObject(jsonObject)
        }.getOrElse {
            fallback(content)
        }
    }

    private fun parseJsonObject(jsonObject: JsonObject): AiResponse {
        return when (jsonObject.get("responseType")?.asString?.lowercase()) {
            "action" -> parseAction(jsonObject)
            "chat" -> AiResponse(
                responseType = AiResponse.ResponseType.CHAT,
                text = jsonObject.get("text")?.asString ?: "我已收到，请继续补充信息。"
            )
            else -> fallback(gson.toJson(jsonObject))
        }
    }

    private fun parseAction(jsonObject: JsonObject): AiResponse {
        val actionName = jsonObject.get("action")?.asString?.lowercase()
        val actionType = when (actionName) {
            "navigate_to" -> AiAction.ActionType.NAVIGATE_TO
            "show_route" -> AiAction.ActionType.SHOW_ROUTE
            "ask_permission" -> AiAction.ActionType.ASK_PERMISSION
            "clarify" -> AiAction.ActionType.CLARIFY
            "show_weather" -> AiAction.ActionType.SHOW_WEATHER
            "show_timetable_navigation" -> AiAction.ActionType.SHOW_TIMETABLE_NAVIGATION
            else -> return fallback("AI 返回了暂不支持的操作，请换一种方式描述需求。")
        }
        val payloadObject = jsonObject.getAsJsonObject("payload") ?: JsonObject()
        val payload = gson.fromJson(payloadObject, Map::class.java) as? Map<String, Any?> ?: emptyMap()
        return AiResponse(
            responseType = AiResponse.ResponseType.ACTION,
            action = AiAction(action = actionType, payload = payload),
            text = actionText(actionType, payload)
        )
    }

    private fun actionText(actionType: AiAction.ActionType, payload: Map<String, Any?>): String {
        return when (actionType) {
            AiAction.ActionType.NAVIGATE_TO -> {
                val target = payload["targetName"]?.toString()
                    ?: payload["fallbackQuery"]?.toString()
                    ?: "目标地点"
                "已为你生成前往 $target 的导航请求。"
            }
            AiAction.ActionType.SHOW_ROUTE -> "已为你生成路线方案。"
            AiAction.ActionType.ASK_PERMISSION -> payload["reason"]?.toString()
                ?: "需要相关权限才能继续。"
            AiAction.ActionType.CLARIFY -> payload["question"]?.toString()
                ?: "请补充更完整的信息。"
            AiAction.ActionType.SHOW_WEATHER -> "正在为你查询天气信息..."
            AiAction.ActionType.SHOW_TIMETABLE_NAVIGATION -> "正在为你生成课表导航建议..."
        }
    }

    private fun extractFirstJsonObject(content: String): String {
        if (content.startsWith("{") && content.endsWith("}")) return content

        val start = content.indexOf('{')
        require(start >= 0) { "No JSON object found" }

        var depth = 0
        var inString = false
        var escaped = false
        for (index in start until content.length) {
            val char = content[index]
            when {
                escaped -> escaped = false
                char == '\\' && inString -> escaped = true
                char == '"' -> inString = !inString
                !inString && char == '{' -> depth++
                !inString && char == '}' -> {
                    depth--
                    if (depth == 0) return content.substring(start, index + 1)
                }
            }
        }
        error("Unclosed JSON object")
    }

    private fun fallback(text: String): AiResponse {
        return AiResponse(
            responseType = AiResponse.ResponseType.CHAT,
            text = text.ifBlank { "AI 助手暂时不可用，请稍后再试。" }
        )
    }
}
