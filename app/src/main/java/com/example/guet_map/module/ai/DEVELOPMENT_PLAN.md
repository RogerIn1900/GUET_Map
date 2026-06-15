# GUET Map `module/ai` 逐文件开发清单

> 目标：在不修改其他模块源码的前提下，只在 `module/ai` 内完成 AI 助手与 AI 课表导航能力。
> 接入模型：`deepseek-chat`
> 核心要求：输出严格 JSON、失败安全降级、信息不足时追问、不会影响地图/搜索/定位等功能。

---

## 一、当前现状

当前 `module/ai` 已存在的基础文件包括：

- `data/model/AiResponse.kt`
- `data/local/AiPromptProvider.kt`
- `domain/parser/AiResponseParser.kt`
- `domain/service/AiService.kt`
- `domain/service/AiServiceImpl.kt`
- `domain/usecase/SendMessageUseCase.kt`
- `ui/chat/ChatViewModel.kt`
- `data/repository/ChatRepository.kt`
- `data/remote/DeepSeekModels.kt`
- `di/AiModule.kt`

其中 `AiServiceImpl` 目前还是 mock 逻辑，后续应替换为真实 DeepSeek 调用。

---

## 二、推荐开发顺序

### 第 1 步：定协议
先保证 AI 返回格式统一。

要做的事：
- 固定 `responseType`
- 固定 `action`
- 固定 `payload`
- 固定 `chat.text`

### 第 2 步：接 DeepSeek
实现真实网络调用。

要做的事：
- 组装 messages
- 读取 API Key
- 发起 HTTP 请求
- 处理超时与重试

### 第 3 步：做解析器
把模型输出转成统一结构。

要做的事：
- 解析 JSON
- 容错处理
- 非 JSON 时安全降级

### 第 4 步：做业务编排
把聊天、课表、上下文串起来。

要做的事：
- 组装上下文
- 保存历史消息
- 调用 AI 服务

### 第 5 步：做 UI 事件层
把 AI 动作转成页面事件。

要做的事：
- chat 直接显示
- navigate_to 发导航事件
- clarify 发追问事件
- show_route 发路线展示事件

---

## 三、文件级开发清单

### 1. `data/model/AiResponse.kt`

#### 作用
定义 DeepSeek 返回内容的统一结构。

#### 应包含
- `responseType`
- `text`
- `action`
- `payload`
- `route`
- `question`
- `permission`

#### 开发要求
- 结构简单
- 便于 JSON 反序列化
- 仅用于 `module/ai`

#### 可交给其他 AI 的任务说明
> 请定义 AI 响应数据模型，支持 `chat`、`action`、`clarify`、`show_route`、`ask_permission` 等结构，要求字段清晰、可解析、仅限 `module/ai` 内使用。

---

### 2. `data/local/AiPromptProvider.kt`

#### 作用
集中管理系统提示词和上下文提示词。

#### 应包含
- `SYSTEM_PROMPT`
- `buildMessages()`
- `buildContextPrompt()`

#### 开发要求
- 严格控制输出 JSON
- 支持普通聊天和课表导航
- 不暴露敏感信息

#### 可交给其他 AI 的任务说明
> 请实现一个提示词提供器，负责生成 DeepSeek 所需的 system prompt 和 user prompt，上下文中要包含用户输入、当前位置、课表信息，并要求模型严格输出 JSON。

---

### 3. `domain/parser/AiResponseParser.kt`

#### 作用
将 DeepSeek 原始输出解析成统一响应对象。

#### 应包含
- JSON 解析
- 首个 JSON 对象抽取
- 非 JSON 降级
- action 文本兜底

#### 开发要求
- 优先解析严格 JSON
- 解析失败时不抛异常
- 失败时返回 chat 类型兜底

#### 可交给其他 AI 的任务说明
> 请实现一个 DeepSeek 响应解析器，要求优先解析标准 JSON，如果返回文本中夹杂解释内容，也要尝试抽取 JSON；若失败则降级为普通 chat，不要中断主流程。

---

### 4. `domain/service/AiService.kt`

#### 作用
定义 AI 服务接口。

#### 应包含
- 发送消息
- 流式发送
- 生成引导问题

#### 开发要求
- 只定义抽象能力
- 不包含网络实现细节

#### 可交给其他 AI 的任务说明
> 请定义 AI 服务接口，抽象出消息发送、流式响应和引导问题生成等能力，要求接口设计简洁，便于后续替换 DeepSeek 实现。

---

### 5. `domain/service/AiServiceImpl.kt`

#### 作用
实现 DeepSeek 真实调用逻辑。

#### 应包含
- DeepSeek HTTP 请求
- 请求头和请求体构建
- 超时控制
- 失败重试
- 调用结果保存

#### 开发要求
- 模型名使用 `deepseek-chat`
- API Key 不可硬编码
- 失败时安全降级
- 不要影响其他模块

#### 可交给其他 AI 的任务说明
> 请将 `AiServiceImpl` 从 mock 实现替换为真实 DeepSeek 调用，要求支持 `deepseek-chat`、超时、一次重试和安全降级，不要修改其他模块代码。

---

### 6. `domain/usecase/SendMessageUseCase.kt`

#### 作用
组织一次完整 AI 对话流程。

#### 应包含
- 用户消息保存
- 历史消息读取
- 上下文拼接
- AI 服务调用
- 结果写回仓库

#### 开发要求
- 负责业务编排
- 不直接处理网络细节
- 不直接操作 UI

#### 可交给其他 AI 的任务说明
> 请实现发送消息用例，负责把用户输入、聊天历史和课表上下文一起发送给 AI 服务，并把返回结果保存为聊天记录，保持与 UI 和网络层解耦。

---

### 7. `domain/usecase/GetChatHistoryUseCase.kt`

#### 作用
读取聊天历史。

#### 应包含
- 按 sessionId 获取历史消息
- 历史消息格式转换

#### 开发要求
- 简单稳定
- 不涉及网络逻辑

#### 可交给其他 AI 的任务说明
> 请实现聊天历史读取用例，输入 sessionId 后返回该会话的历史消息列表，要求与数据仓库解耦。

---

### 8. `ui/chat/ChatViewModel.kt`

#### 作用
管理聊天界面的状态和事件。

#### 应包含
- 发送消息
- 更新消息列表
- 处理 loading 状态
- 分发 AI action 事件

#### 开发要求
- chat 直接显示
- navigate_to 触发导航事件
- clarify 触发追问展示
- show_route 触发路线展示

#### 可交给其他 AI 的任务说明
> 请实现聊天页面 ViewModel，要求能够处理 AI 返回的 chat、navigate_to、show_route、clarify 等结果，并通过事件机制通知宿主页面，不要修改地图模块。

---

### 9. `ui/chat/ChatFragment.kt`

#### 作用
承载聊天页面 UI。

#### 应包含
- 消息输入
- 消息列表
- 发送按钮
- AI 响应展示

#### 开发要求
- 仅负责展示
- 不做复杂业务逻辑

#### 可交给其他 AI 的任务说明
> 请实现聊天页面 Fragment 的 UI 绑定逻辑，只负责展示消息、发送输入和接收 ViewModel 状态，不要把业务逻辑写进 Fragment。

---

### 10. `data/repository/ChatRepository.kt`

#### 作用
定义聊天记录的数据访问接口。

#### 应包含
- 保存消息
- 读取历史
- 清空历史

#### 开发要求
- 保持接口稳定
- 不耦合 UI

#### 可交给其他 AI 的任务说明
> 请定义聊天记录仓库接口，支持保存、读取和清空消息历史，要求只服务于 `module/ai` 内部使用。

---

### 11. `data/remote/DeepSeekModels.kt`

#### 作用
定义 DeepSeek 请求和响应模型。

#### 应包含
- `DeepSeekRequest`
- `DeepSeekMessage`
- `DeepSeekResponse`
- `DeepSeekChoice`

#### 开发要求
- 与 API 文档对齐
- 结构轻量

#### 可交给其他 AI 的任务说明
> 请定义 DeepSeek 请求和响应的数据模型，要求适配 `deepseek-chat` 接口，尽量保持字段精简并方便序列化。

---

### 12. `di/AiModule.kt`

#### 作用
管理依赖注入。

#### 应包含
- `AiService`
- `AiResponseParser`
- `SendMessageUseCase`
- `ChatRepository`
- `AiPromptProvider`

#### 开发要求
- 只绑定 AI 模块内部依赖
- 不引入跨模块强耦合

#### 可交给其他 AI 的任务说明
> 请实现 `module/ai` 的依赖注入配置，只注入 AI 功能所需组件，不要改动其他模块的 DI 结构。

---

### 13. `data/local/dao/ChatMessageDao.kt`

#### 作用
处理聊天消息本地存储。

#### 应包含
- 插入消息
- 查询历史
- 清空会话消息

#### 开发要求
- 与数据库实体配合
- 保持查询简单稳定

#### 可交给其他 AI 的任务说明
> 请实现聊天消息 DAO，支持插入、查询和删除历史消息，要求与 Room 或现有本地存储结构兼容。

---

### 14. `data/local/entity/ChatMessageEntity.kt`

#### 作用
定义聊天消息本地实体。

#### 应包含
- sessionId
- role
- content
- timestamp

#### 开发要求
- 与 DAO 配套
- 字段尽量简洁

#### 可交给其他 AI 的任务说明
> 请定义聊天消息数据库实体，要求能保存对话会话标识、消息角色、内容和时间戳。

---

## 四、推荐的代码骨架顺序

### 优先级 1
- `AiResponse.kt`
- `DeepSeekModels.kt`
- `AiPromptProvider.kt`

### 优先级 2
- `AiResponseParser.kt`
- `AiService.kt`
- `AiServiceImpl.kt`

### 优先级 3
- `ChatRepository.kt`
- `SendMessageUseCase.kt`
- `GetChatHistoryUseCase.kt`

### 优先级 4
- `ChatViewModel.kt`
- `ChatFragment.kt`
- `AiModule.kt`

---

## 五、总任务模板

如果要一次性发给其他 AI，可直接使用下面这段：

> 请只在 `module/ai` 内实现 GUET Map 的 AI 助手与 AI 课表导航功能。要求如下：
> 1. 接入 DeepSeek `deepseek-chat`
> 2. 输出必须严格 JSON
> 3. 支持 `chat`、`action`、`clarify`、`show_route`、`ask_permission`
> 4. 不修改其他模块代码
> 5. 不影响地图、搜索、定位等功能
> 6. API Key 不可硬编码
> 7. 模型失败时必须有兜底
> 8. 仅在 `module/ai` 内部完成实现

---

## 六、执行原则

1. 先定协议，再写实现。
2. 先写模型，再写服务。
3. 先写解析，再写业务。
4. 先保证兜底，再考虑优化。
5. 所有代码变更限制在 `module/ai`。

---

## 七、阶段里程碑

### 里程碑 1：文档与协议稳定

目标：先把 AI 模块的行为边界、输出协议和异常兜底规则固定下来。

完成标准：
- `README.md` 已说明 AI 助手与课表导航目标
- `DEVELOPMENT_PLAN.md` 已明确逐文件职责
- `CODE_SKELETON.md` 已给出核心代码骨架
- DeepSeek 输出协议已固定为 `chat` / `action`

当前状态：已完成。

---

### 里程碑 2：保持现有聊天 UI 可用

目标：在不破坏现有聊天页面的情况下，把 mock AI 服务替换为可扩展的真实 AI 服务结构。

建议完成内容：
- 保持 `AiService` 当前接口不变
- 保持 `SendMessageUseCase` 当前调用方式不变
- 保持 `ChatViewModel` 当前 UI 状态逻辑不变
- 在 `AiServiceImpl` 内部逐步接入 DeepSeek

验收标准：
- 用户仍可发送消息
- AI 回复能保存到聊天历史
- 网络失败时不会崩溃
- 未配置 API Key 时能返回友好提示

---

### 里程碑 3：接入 DeepSeek `deepseek-chat`

目标：让 `AiServiceImpl` 能真实调用 DeepSeek。

建议完成内容：
- 使用 `AiPromptProvider` 组装 messages
- 使用 `DeepSeekModels.kt` 定义请求和响应结构
- 从安全配置读取 API Key
- 设置 10 秒左右超时
- 失败时最多短重试一次
- 响应内容交给 `AiResponseParser` 处理

验收标准：
- 可以发起真实请求
- 模型名称为 `deepseek-chat`
- API Key 不出现在源码、日志、prompt 或提交记录中
- 5xx、超时、解析失败都有兜底

---

### 里程碑 4：结构化解析与动作识别

目标：让模型返回的 JSON 能被稳定解析成 `AiResponse`。

建议完成内容：
- 优先解析严格 JSON
- 如果模型夹杂自然语言，尝试抽取第一个 JSON 对象
- 支持 `navigate_to`
- 支持 `show_route`
- 支持 `ask_permission`
- 支持 `clarify`
- 不支持的 action 降级为 chat

验收标准：
- 标准 JSON 能解析成功
- 非标准文本不会导致崩溃
- action 能生成可展示的兜底文案
- clarify 能正确显示追问内容

---

### 里程碑 5：课表导航上下文增强

目标：让用户说“带我去下一节课”“我要去上课”等话时，AI 能根据上下文返回导航动作。

建议完成内容：
- 在 prompt 中明确课表上下文格式
- 如果有课程名、教室、上课时间，优先生成导航意图
- 如果只有课程名但缺少地点，返回 `clarify`
- 如果只有教室号但缺少楼栋，谨慎追问

验收标准：
- “带我去下一节课”能生成 `navigate_to` 或 `clarify`
- “我要去教科楼B 204”能生成 `navigate_to`
- “我要去上课”但无课表信息时能追问
- 不确定地点时不会编造路线

---

### 里程碑 6：UI 事件与宿主交互准备

目标：让 AI 模块内部能把 action 转成事件，但不直接修改地图模块。

建议完成内容：
- 在 `module/ai/ui/chat` 内定义 `ChatUiEvent`
- `ChatViewModel` 根据 action 生成事件
- `ChatFragment` 只监听和转发事件
- 宿主是否处理导航事件由现有机制决定

验收标准：
- chat 正常显示
- clarify 正常展示为追问
- navigate_to 能形成事件对象
- show_route 能形成路线展示事件
- AI 模块不直接调用地图模块内部方法

---

## 八、验收用例

### 普通聊天

用户输入：
> 你好

期望：
```json
{"responseType":"chat","text":"你好，我是 GUET Map 校园 AI 助手，可以帮你查询地点、路线和课表导航。"}
```

---

### 校园地点问答

用户输入：
> 图书馆在哪里？

期望：
- 如果只回答说明，返回 `chat`
- 如果能导航，返回 `navigate_to`
- 不要返回与校园无关的长篇解释

---

### 明确地点导航

用户输入：
> 带我去教科楼B 204

期望：
```json
{"responseType":"action","action":"navigate_to","payload":{"targetName":"教科楼B 204","targetLocationId":null,"fallbackQuery":"教科楼B","mode":"walking"}}
```

---

### 课表导航

上下文：
- 当前时间：上午 9:40
- 下一节课：数据结构
- 上课地点：教科楼B 204

用户输入：
> 带我去下一节课

期望：
```json
{"responseType":"action","action":"navigate_to","payload":{"targetName":"教科楼B 204","targetLocationId":null,"fallbackQuery":"教科楼B","mode":"walking"}}
```

---

### 信息不足

用户输入：
> 带我去上课

上下文：
- 无课表信息
- 无目标地点

期望：
```json
{"responseType":"action","action":"clarify","payload":{"question":"请告诉我下一节课的教室或课程地点，例如教科楼B 204。"}}
```

---

### 权限不足

用户输入：
> 从我现在的位置去教室

上下文：
- 无当前位置
- 未授权定位

期望：
```json
{"responseType":"action","action":"ask_permission","payload":{"permission":"location","reason":"需要定位权限以规划从当前位置到教室的步行路线。"}}
```

---

## 九、风险控制清单

### API Key 风险

必须避免：
- 写死在 Kotlin 文件中
- 写入 README 示例中的真实值
- 打印到日志
- 传给模型作为上下文
- 提交到 Git

建议方式：
- `local.properties`
- `BuildConfig`
- Android Keystore
- 后端代理

---

### 模型输出风险

可能问题：
- 返回非 JSON
- 返回 markdown 代码块
- 返回额外解释文字
- action 字段拼写错误
- payload 字段缺失

应对方式：
- `AiResponseParser` 抽取首个 JSON 对象
- 解析失败降级为 `chat`
- 不支持的 action 降级为友好提示
- payload 缺失时使用默认值

---

### 导航误导风险

可能问题：
- 模型编造不存在地点
- 教室楼栋不明确
- 当前位置缺失
- 课表数据过期

应对方式：
- prompt 中明确“不确定必须追问”
- 对 `targetName` 优先走本地地点解析
- 无法解析时显示澄清问题
- 不直接把模型路线当作唯一事实来源

---

### 跨模块影响风险

必须避免：
- 修改地图模块
- 修改搜索模块
- 修改定位模块
- 直接调用其他模块内部实现
- 改动全局依赖注入导致其他功能异常

建议方式：
- action 通过事件向外抛出
- AI 模块只生成意图，不直接执行地图逻辑
- 需要宿主处理时，由现有页面或导航机制承接

---

## 十、逐文件完成状态建议

| 文件 | 当前建议状态 | 下一步 |
| --- | --- | --- |
| `README.md` | 已规划 | 后续随实现更新 |
| `DEVELOPMENT_PLAN.md` | 已规划 | 作为开发执行清单 |
| `CODE_SKELETON.md` | 已规划 | 作为代码实现参考 |
| `data/model/AiResponse.kt` | 已有基础结构 | 可增强强类型 payload |
| `data/local/AiPromptProvider.kt` | 已有基础提示词 | 可加入更完整课表上下文 |
| `data/remote/DeepSeekModels.kt` | 已有文件 | 检查字段是否与 DeepSeek API 对齐 |
| `domain/parser/AiResponseParser.kt` | 已有较完整解析 | 可增强代码块 JSON 抽取 |
| `domain/service/AiService.kt` | 已有接口 | 暂时保持兼容 |
| `domain/service/AiServiceImpl.kt` | 当前 mock | 替换为真实 DeepSeek 调用 |
| `domain/usecase/SendMessageUseCase.kt` | 已有文件 | 增强上下文传递 |
| `ui/chat/ChatViewModel.kt` | 已有文件 | 后续增加 action 事件处理 |
| `di/AiModule.kt` | 已有文件 | 补充 parser、prompt、client 注入 |

---

## 十一、结论

这份清单的目的，是让后续实现可以按文件逐个推进，避免一次性改动过大、影响其他模块或引入新问题。

推荐最终执行路线：
1. 保持现有聊天功能不变。
2. 先把 DeepSeek 网络调用接入 `AiServiceImpl`。
3. 再用 `AiResponseParser` 做结构化解析。
4. 然后增强 prompt 的课表上下文。
5. 最后再通过 UI 事件对接导航意图。
