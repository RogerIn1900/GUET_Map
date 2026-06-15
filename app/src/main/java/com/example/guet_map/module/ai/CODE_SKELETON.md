# GUET Map `module/ai` 代码骨架建议

> 本文件用于指导后续实现 AI 助手与 AI 课表导航功能。
> 目标是先稳定接口和职责边界，再逐步替换 mock 实现。

---

## 一、数据模型骨架

### 目标

先把 AI 模块内部的返回结果、动作类型、路线信息定义清楚，这样后续 `AiResponseParser`、`ChatViewModel`、`AiServiceImpl` 都可以围绕同一套结构工作。

### `data/model/AiResponse.kt`

建议先使用一个稳定、易解析、兼容当前实现的结构。

```kotlin
data class AiResponse(
    val responseType: ResponseType,
    val text: String? = null,
    val action: AiAction? = null
) {
    enum class ResponseType {
        CHAT,
        ACTION
    }
}

data class AiAction(
    val action: ActionType,
    val payload: Map<String, Any?> = emptyMap()
) {
    enum class ActionType {
        NAVIGATE_TO,
        SHOW_ROUTE,
        ASK_PERMISSION,
        CLARIFY
    }
}
```

### 推荐补充的强类型 payload

如果后续要减少 `Map<String, Any?>` 带来的解析风险，建议把常见 action 的 payload 再拆成更强类型：

```kotlin
data class NavigatePayload(
    val targetName: String?,
    val targetLocationId: String?,
    val fallbackQuery: String?,
    val mode: String = "walking"
)

data class ClarifyPayload(
    val question: String
)

data class PermissionPayload(
    val permission: String,
    val reason: String
)

data class RoutePayload(
    val route: AiRoute
)

data class AiRoute(
    val summary: String,
    val distance: Int,
    val durationMin: Int,
    val steps: List<AiRouteStep>
)

data class AiRouteStep(
    val desc: String,
    val lat: Double,
    val lng: Double
)
```

### 推荐落地方式

- 当前实现先保持 `Map<String, Any?>`，保证兼容性。
- 当 `navigate_to` 和 `show_route` 的字段稳定后，再逐步替换成强类型 payload。
- `responseType` 和 `action` 的值尽量保持与 prompt 完全一致，避免大小写或拼写不一致导致解析失败。

---

## 二、DeepSeek 请求响应骨架

### 目标

这一层只负责和 DeepSeek 通信，不处理聊天业务逻辑，也不处理 UI。核心目标是：
- 请求格式和 DeepSeek API 对齐
- 响应格式便于解析
- 便于后面替换成真实网络实现

### `data/remote/DeepSeekModels.kt`

建议结构如下：

```kotlin
data class DeepSeekRequest(
    val model: String = "deepseek-chat",
    val messages: List<DeepSeekMessage>,
    val temperature: Double = 0.2,
    val max_tokens: Int = 1024,
    val stream: Boolean = false
)

data class DeepSeekMessage(
    val role: String,
    val content: String
)

data class DeepSeekResponse(
    val choices: List<DeepSeekChoice> = emptyList(),
    val usage: DeepSeekUsage? = null
)

data class DeepSeekChoice(
    val index: Int? = null,
    val message: DeepSeekMessage? = null,
    val finish_reason: String? = null
)

data class DeepSeekUsage(
    val prompt_tokens: Int? = null,
    val completion_tokens: Int? = null,
    val total_tokens: Int? = null
)
```

### 推荐说明

- `temperature` 保持较低，减少模型乱写和格式漂移。
- `max_tokens` 不宜过大，避免返回过长内容和超时。
- `stream` 先保留字段，方便未来扩展流式输出。
- `usage` 字段不是必须，但建议保留，便于后续做调试和成本统计。

### 建议额外补一个请求常量文件

如果后续要统一配置模型名、超时时间、URL，建议新增：

```kotlin
object DeepSeekConstants {
    const val MODEL = "deepseek-chat"
    const val BASE_URL = "https://api.deepseek.com"
    const val TIMEOUT_MS = 10_000L
    const val MAX_RETRY = 1
}
```

### 落地建议

- 请求结构保持和 DeepSeek 官方接口命名一致。
- 不要在模型请求中夹杂 UI 文本或多余上下文。
- 由 `AiPromptProvider` 负责整理上下文，`DeepSeekModels.kt` 只管承载数据。

---

## 三、提示词提供器骨架

### 目标

这一层负责把用户输入、课表上下文、当前位置、历史消息整理成给模型看的内容。它的核心任务不是“理解内容”，而是“组织内容”。

### `data/local/AiPromptProvider.kt`

建议职责：

```kotlin
class AiPromptProvider {

    fun buildMessages(
        userMessage: String,
        locationContext: String?,
        history: List<Pair<String, String>> = emptyList(),
        timetableContext: String? = null
    ): List<DeepSeekMessage> {
        return buildList {
            add(DeepSeekMessage("system", SYSTEM_PROMPT))
            history.takeLast(MAX_HISTORY_MESSAGES).forEach { (role, content) ->
                add(DeepSeekMessage(role, content))
            }
            add(
                DeepSeekMessage(
                    role = "user",
                    content = buildContextPrompt(
                        userMessage = userMessage,
                        locationContext = locationContext,
                        timetableContext = timetableContext
                    )
                )
            )
        }
    }

    private fun buildContextPrompt(
        userMessage: String,
        locationContext: String?,
        timetableContext: String?
    ): String {
        return """
        用户输入：
        $userMessage

        当前上下文：
        - 当前地点或地点上下文：${locationContext ?: "未知"}
        - 当前时间戳：${System.currentTimeMillis()}
        - 课表信息：${timetableContext ?: "无"}

        处理要求：
        1. 如果用户提到课程、教室、上课时间、下一节课或到课路线，优先结合课表信息判断是否应返回导航动作。
        2. 如果地点不完整或无法可靠推断，必须返回 clarify，向用户追问缺失信息。
        3. 如果可以直接确定目标地点，优先返回 navigate_to 或 show_route。
        4. 所有输出必须符合系统提示词要求的 JSON 格式。
        """.trimIndent()
    }

    companion object {
        private const val MAX_HISTORY_MESSAGES = 8

        const val SYSTEM_PROMPT = """
        这里放 README 中整理后的最终系统提示词。
        """
    }
}
```

### 落地建议

- `history` 只保留最近几轮，避免 prompt 过长。
- `timetableContext` 建议由上层整理成一段简洁文本，不要直接塞原始大对象。
- 如果当前页面有导航意图，建议在 prompt 中明确说明“优先输出 action”。

---

## 四、响应解析器骨架

### 目标

这一层负责把模型返回的文本变成可执行的结构化对象。它必须是整个 AI 模块里最稳的一层之一，因为模型输出不可能 100% 规整。

### `domain/parser/AiResponseParser.kt`

建议职责：

```kotlin
class AiResponseParser(
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
        // 根据 responseType 判断 chat 或 action
        TODO("parse responseType")
    }

    private fun extractFirstJsonObject(content: String): String {
        // 从模型输出中抽取第一个完整 JSON 对象
        TODO("extract json")
    }

    private fun fallback(text: String): AiResponse {
        return AiResponse(
            responseType = AiResponse.ResponseType.CHAT,
            text = text.ifBlank { "AI 助手暂时不可用，请稍后再试。" }
        )
    }
}
```

### 建议增强点

1. **严格区分 chat / action**
   - `responseType = chat` 时，只读取 `text`
   - `responseType = action` 时，只读取 `action + payload`

2. **兼容模型多余输出**
   - 如果模型包了 markdown 代码块，优先抽取 JSON 内容
   - 如果模型前后夹杂说明文字，优先抽取第一个完整 JSON 对象

3. **payload 解析要容错**
   - 如果 payload 缺失，使用空对象或默认值
   - 如果 action 不在允许范围内，降级为 chat

4. **不要把解析异常抛给 UI**
   - 解析失败必须返回 fallback
   - fallback 内容建议尽量保留原文，方便调试

### 推荐的解析顺序

1. 去掉首尾空白
2. 尝试直接解析完整 JSON
3. 如果失败，抽取第一个 JSON 对象再解析
4. 再失败就降级为 chat 文本

### 落地建议

- 当前项目中已经有初版解析器，可以先在此基础上增强健壮性。
- 后续如果 action 越来越多，建议把 payload 再拆成强类型对象。
- 如果后面要支持流式输出，解析器需要额外区分“局部内容”和“最终内容”。

---

## 五、AI 服务接口骨架

### 目标

这一层对外提供 AI 能力入口。建议先保持和现有项目兼容，避免一下子把 UI 和业务层全部改掉。

### `domain/service/AiService.kt`

建议结构：

```kotlin
interface AiService {

    suspend fun sendMessage(
        sessionId: String,
        userMessage: String,
        locationContext: String? = null
    ): Resource<ChatMessage>

    fun sendMessageStream(
        sessionId: String,
        userMessage: String,
        locationContext: String? = null
    ): Flow<Resource<String>>

    suspend fun generateGuidedQuestions(
        locationId: String? = null
    ): Resource<List<String>>
}
```

### 后续可扩展方向

如果后续要把 `action` 直接传给 UI，而不是只转换成文本，建议再补一个接口方法，例如：

```kotlin
suspend fun sendStructuredMessage(
    sessionId: String,
    userMessage: String,
    locationContext: String? = null,
    timetableContext: String? = null
): Resource<AiResponse>
```

### 落地建议

- 当前阶段优先保证兼容性，不建议直接重构所有调用链。
- 先让 `sendMessage` 保持可用，再逐步引入结构化结果。
- 如果 `sendMessage` 最终返回的是 `ChatMessage`，那就让 `AiResponseParser` 的结果先落成文本；如果后续有事件通道，再把 `action` 单独输出。

---

## 六、AI 服务实现骨架

### 目标

这一层是真正把模型接进来的地方。建议把“请求 DeepSeek”“解析结果”“落库聊天记录”分成清晰步骤，这样后续排查问题会容易很多。

### `domain/service/AiServiceImpl.kt`

建议最终职责：

```kotlin
class AiServiceImpl(
    private val chatRepository: ChatRepository,
    private val promptProvider: AiPromptProvider,
    private val responseParser: AiResponseParser,
    private val gson: Gson,
    private val apiKeyProvider: ApiKeyProvider
) : AiService {

    override suspend fun sendMessage(
        sessionId: String,
        userMessage: String,
        locationContext: String?
    ): Resource<ChatMessage> {
        return try {
            // 1. 保存用户消息
            chatRepository.saveMessage(sessionId, ChatRole.USER, userMessage)

            // 2. 组织 prompt
            val messages = promptProvider.buildMessages(
                userMessage = userMessage,
                locationContext = locationContext,
                history = emptyList()
            )

            // 3. 请求 DeepSeek
            val rawContent = callDeepSeek(messages)

            // 4. 解析结果
            val parsed = responseParser.parse(rawContent)
            val displayText = parsed.text ?: "已收到 AI 返回结果。"

            // 5. 保存 AI 回复
            val assistantMessage = chatRepository.saveMessage(
                sessionId = sessionId,
                role = ChatRole.ASSISTANT,
                content = displayText,
                locationId = locationContext
            )

            Resource.Success(assistantMessage)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "AI 服务异常")
        }
    }

    private suspend fun callDeepSeek(messages: List<DeepSeekMessage>): String {
        // 使用 OkHttp、Retrofit 或项目已有网络栈调用 DeepSeek
        TODO("DeepSeek HTTP call")
    }
}
```

### 建议实现步骤

#### 1）请求前准备
- 从 `ApiKeyProvider` 读取 key
- 从 `DeepSeekConfigProvider` 读取 baseUrl 和 model
- 组装 HTTP 请求头

#### 2）发送请求
- 使用 POST
- 请求体采用 `DeepSeekRequest`
- 超时时间建议 10 秒左右
- 失败时最多重试一次

#### 3）解析返回
- 先取出模型的 message content
- 交给 `AiResponseParser` 解析
- 解析不到就降级为文本回复

#### 4）保存结果
- 用户消息和 AI 消息都写入 `ChatRepository`
- 如果后面支持 action，可考虑单独保存一份结构化结果

### 实现要点

- 不硬编码 API Key。
- 请求失败最多短重试一次。
- 失败后返回友好错误。
- 不要把原始异常直接暴露给用户。
- 不要让 `AiServiceImpl` 既做网络、又做复杂 UI 判断、又做业务路由。

### 推荐拆法

如果后续文件过长，建议把请求逻辑再拆成：
- `DeepSeekRemoteDataSource`
- `AiServiceImpl`

其中 `DeepSeekRemoteDataSource` 只负责 HTTP 请求，`AiServiceImpl` 负责业务编排。

---

## 七、API Key Provider 骨架

### 目标

这一层只负责提供 DeepSeek 接入所需配置，不负责网络请求，也不负责业务逻辑。

### 建议新增 `data/remote/DeepSeekConfigProvider.kt`

```kotlin
interface DeepSeekConfigProvider {
    fun getApiKey(): String?
    fun getBaseUrl(): String = "https://api.deepseek.com"
    fun getModel(): String = "deepseek-chat"
}
```

### 推荐补充实现类

```kotlin
class DefaultDeepSeekConfigProvider(
    private val apiKey: String?
) : DeepSeekConfigProvider {
    override fun getApiKey(): String? = apiKey
}
```

### 说明

- 可先从 `BuildConfig`、本地配置或安全存储读取。
- 不要在源码中写死真实 key。
- 如果没有 key，AI 服务应返回友好提示，而不是直接崩溃。
- 由于这是敏感信息，日志里也不要输出明文 key。

### 落地建议

- 先把配置提供器做成可替换实现。
- 后续如果改成后端代理，也只需要替换 provider，不需要改业务层。

---

## 八、业务用例骨架

### 目标

这一层负责把“用户输入”转换成“AI 调用动作”，是最适合放业务规则的位置，但不要把网络请求或 UI 逻辑塞进去。

### `domain/usecase/SendMessageUseCase.kt`

建议职责：

```kotlin
class SendMessageUseCase(
    private val aiService: AiService
) {
    suspend operator fun invoke(
        sessionId: String,
        message: String,
        locationContext: String? = null,
        timetableContext: String? = null
    ): Resource<ChatMessage> {
        if (message.isBlank()) {
            return Resource.Error("请输入内容")
        }
        return aiService.sendMessage(sessionId, message.trim(), locationContext)
    }
}
```

### 建议增强点

1. **输入校验**
   - 空消息直接返回错误
   - 过长消息可考虑截断或提示

2. **上下文传递**
   - `locationContext` 用于位置相关问题
   - `timetableContext` 用于课表导航问题
   - 如果后续要支持更多上下文，可继续扩展参数，但不要让接口变得太臃肿

3. **职责边界**
   - UseCase 只负责调用链编排
   - 不负责解析 JSON
   - 不负责决定 action 如何执行

### 落地建议

- 当前阶段先保持 `SendMessageUseCase` 简单稳定。
- 如果后续需要把结构化 action 直接返回给上层，可以再增加一个 `SendStructuredMessageUseCase` 或扩展返回类型。

---

## 九、UI 事件骨架

建议新增：

### `ui/chat/ChatUiEvent.kt`

```kotlin
sealed class ChatUiEvent {
    data class NavigateTo(
        val targetName: String?,
        val targetLocationId: String?,
        val fallbackQuery: String?,
        val mode: String = "walking"
    ) : ChatUiEvent()

    data class ShowRoute(
        val summary: String
    ) : ChatUiEvent()

    data class AskPermission(
        val permission: String,
        val reason: String
    ) : ChatUiEvent()

    data class ShowClarifyQuestion(
        val question: String
    ) : ChatUiEvent()

    data class ShowMessage(
        val message: String
    ) : ChatUiEvent()
}
```

说明：
- 事件只在 AI 模块内部定义。
- 宿主页面是否响应事件，由已有机制决定。
- 不要直接调用其他模块内部方法。

---

## 十、ViewModel 骨架

### `ui/chat/ChatViewModel.kt`

建议职责：

```kotlin
class ChatViewModel(
    private val sendMessageUseCase: SendMessageUseCase
) : ViewModel() {

    fun sendMessage(message: String, locationContext: String?) {
        viewModelScope.launch {
            // 1. 设置 loading
            // 2. 调用 sendMessageUseCase
            // 3. 更新消息列表
            // 4. 如果后续返回 AiResponse，则分发 ChatUiEvent
        }
    }
}
```

后续增强方向：
- 如果 `AiService` 能返回 `AiResponse`，ViewModel 可以根据 action 分发事件。
- 如果暂时只能返回 `ChatMessage`，则先保证文本聊天功能稳定。

---

## 十一、依赖注入骨架

### `di/AiModule.kt`

建议职责：

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AiModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    fun provideAiResponseParser(gson: Gson): AiResponseParser {
        return AiResponseParser(gson)
    }

    @Provides
    @Singleton
    fun provideAiPromptProvider(): AiPromptProvider {
        return AiPromptProvider()
    }
}
```

说明：
- 只注入 AI 模块需要的依赖。
- 不主动改其他模块 DI。

---

## 十二、阶段性实现建议

### 阶段 1：保持现有 UI 可用
- 保留 `AiService` 原接口
- `AiServiceImpl` 从 mock 逐步替换为 DeepSeek
- 输出仍保存为聊天文本

### 阶段 2：引入结构化 action
- `AiServiceImpl` 解析 `AiResponse`
- 将 action 转为可展示文本
- 在 ViewModel 中准备事件通道

### 阶段 3：接入课表导航
- 在 user prompt 中加入课表上下文
- 支持“下一节课”解析
- 生成 `navigate_to` action

### 阶段 4：完善导航事件
- 通过已有宿主事件机制传递导航意图
- 不直接修改地图模块内部实现

---

## 十三、注意事项

1. 不要硬编码 API Key。
2. 不要提交真实密钥。
3. 不要直接修改地图、搜索、定位模块。
4. 不要让 AI 模块异常影响主流程。
5. 模型输出非 JSON 时必须兜底。
6. 不确定地点时必须追问。
7. 优先保证聊天可用，再逐步增强导航动作。
