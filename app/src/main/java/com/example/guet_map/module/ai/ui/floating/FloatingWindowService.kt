package com.example.guet_map.module.ai.ui.floating

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.guet_map.databinding.LayoutFloatingWindowBinding
import com.example.guet_map.model.Resource
import com.example.guet_map.module.ai.data.model.ChatMessage
import com.example.guet_map.module.ai.data.model.ChatRole
import com.example.guet_map.module.ai.data.repository.ChatRepository
import com.example.guet_map.module.ai.domain.service.AiService
import com.example.guet_map.module.ai.ui.chat.ChatMessageAdapter
import com.example.guet_map.module.ai.ui.chat.ChatUiEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * AI 悬浮窗服务
 * 提供悬浮窗形式的 AI 助手界面
 */
@AndroidEntryPoint
class FloatingWindowService : Service(), LifecycleOwner {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var floatingParams: WindowManager.LayoutParams? = null

    private var _binding: LayoutFloatingWindowBinding? = null
    private val binding get() = _binding!!

    private lateinit var lifecycleRegistry: LifecycleRegistry

    @Inject
    lateinit var aiService: AiService

    @Inject
    lateinit var chatRepository: ChatRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private lateinit var messageAdapter: ChatMessageAdapter

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    private val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    private val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _events = MutableSharedFlow<ChatUiEvent>()
    private val events: SharedFlow<ChatUiEvent> = _events.asSharedFlow()

    // 使用固定的会话ID，存储在 SharedPreferences 中以保持会话持久化
    private val prefs by lazy { getSharedPreferences("floating_window_prefs", Context.MODE_PRIVATE) }
    private val sessionId: String
        get() {
            var id = prefs.getString(KEY_SESSION_ID, null)
            if (id == null) {
                id = java.util.UUID.randomUUID().toString()
                prefs.edit().putString(KEY_SESSION_ID, id).apply()
            }
            return id
        }

    private var isExpanded = true
    private val collapsedHeight = 120 // dp
    private val expandedHeight = 400 // dp

    // 触摸事件相关
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    // 是否正在编辑输入框
    private var isInputFocused = false

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override fun onCreate() {
        super.onCreate()
        lifecycleRegistry = LifecycleRegistry(this)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        // 创建通知渠道
        createNotificationChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED

        // 创建前台通知
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AI 助手运行中")
            .setContentText("点击可展开悬浮窗口")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
        startForeground(NOTIFICATION_ID, notification)

        startFloatingWindow()
        return START_STICKY
    }

    private fun startFloatingWindow() {
        if (floatingView != null) return

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        _binding = LayoutFloatingWindowBinding.inflate(LayoutInflater.from(this))
        floatingView = binding.root

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        floatingParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            dpToPx(expandedHeight),
            screenWidth / 2 - dpToPx(140),
            screenHeight / 3,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            windowAnimations = android.R.style.Animation_Dialog
        }

        setupViews()
        loadChatHistory()
        observeState()

        lifecycleRegistry.currentState = Lifecycle.State.RESUMED

        try {
            windowManager?.addView(floatingView, floatingParams)
        } catch (e: Exception) {
            Toast.makeText(this, "无法创建悬浮窗: ${e.message}", Toast.LENGTH_LONG).show()
            stopSelf()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupViews() {
        messageAdapter = ChatMessageAdapter()
        binding.recyclerViewMessages.apply {
            adapter = messageAdapter
            layoutManager = LinearLayoutManager(this@FloatingWindowService).apply {
                stackFromEnd = true
            }
        }

        // 发送按钮
        binding.buttonSend.setOnClickListener {
            val message = binding.editTextMessage.text.toString()
            if (message.isNotBlank()) {
                sendMessage(message)
                binding.editTextMessage.text?.clear()
                hideInputMethod()
            }
        }

        // 关闭按钮
        binding.buttonClose.setOnClickListener {
            stopSelf()
        }

        // 最小化/展开按钮
        binding.buttonMinimize.setOnClickListener {
            toggleWindowSize()
        }

        // 输入框点击 - 请求焦点并显示输入法
        binding.editTextMessage.setOnFocusChangeListener { _, hasFocus ->
            isInputFocused = hasFocus
            updateWindowFocusability()
        }

        binding.editTextMessage.setOnClickListener {
            requestInputFocus()
        }

        // 标题栏拖动功能
        binding.titleBar.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = floatingParams?.x ?: 0
                    initialY = floatingParams?.y ?: 0
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    floatingParams?.let { params ->
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager?.updateViewLayout(floatingView, params)
                    }
                    true
                }
                else -> false
            }
        }
    }

    /**
     * 请求输入框焦点并显示输入法
     */
    private fun requestInputFocus() {
        binding.editTextMessage.requestFocus()
        showInputMethod()
    }

    /**
     * 显示输入法
     */
    private fun showInputMethod() {
        val imm = getSystemService<InputMethodManager>()
        imm?.showSoftInput(binding.editTextMessage, InputMethodManager.SHOW_IMPLICIT)
        isInputFocused = true
        updateWindowFocusability()
    }

    /**
     * 隐藏输入法
     */
    private fun hideInputMethod() {
        val imm = getSystemService<InputMethodManager>()
        imm?.hideSoftInputFromWindow(binding.editTextMessage.windowToken, 0)
        binding.editTextMessage.clearFocus()
        isInputFocused = false
        updateWindowFocusability()
    }

    /**
     * 根据输入框焦点状态更新窗口焦点能力
     * 当需要输入时，移除 FLAG_NOT_FOCUSABLE 以允许输入法显示
     */
    private fun updateWindowFocusability() {
        floatingParams?.let { params ->
            val newFlags = if (isInputFocused) {
                // 需要输入时，移除 FLAG_NOT_FOCUSABLE
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            } else {
                // 不需要输入时，保持不可获取焦点
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            }
            if (params.flags != newFlags) {
                params.flags = newFlags
                windowManager?.updateViewLayout(floatingView, params)
            }
        }
    }

    /**
     * 加载聊天历史记录
     * 只在初始化时加载一次，之后由 Flow 自动同步
     */
    private fun loadChatHistory() {
        serviceScope.launch {
            try {
                // 只加载一次历史记录
                val history = chatRepository.getMessages(sessionId)
                // 通过 observeState 中的 Flow 收集来更新 UI
            } catch (e: Exception) {
                android.util.Log.e("FloatingWindowService", "加载历史记录失败", e)
            }
        }
    }

    private fun observeState() {
        // 从数据库 Flow 收集消息，实现自动同步
        serviceScope.launch {
            chatRepository.getMessages(sessionId).collect { msgs ->
                messageAdapter.submitList(msgs) {
                    scrollToBottom()
                }
            }
        }

        serviceScope.launch {
            isLoading.collect { loading ->
                binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
                binding.buttonSend.isEnabled = !loading
            }
        }

        serviceScope.launch {
            events.collect { event ->
                handleEvent(event)
            }
        }
    }

    /**
     * 滚动到消息列表底部
     */
    private fun scrollToBottom() {
        val itemCount = messageAdapter.itemCount
        if (itemCount > 0) {
            binding.recyclerViewMessages.smoothScrollToPosition(itemCount - 1)
        }
    }

    private fun sendMessage(content: String) {
        if (content.isBlank() || _isLoading.value) return

        android.util.Log.d("FloatingWindowService", "sendMessage called: $content")
        android.util.Log.d("FloatingWindowService", "aiService initialized: ${::aiService.isInitialized}")
        android.util.Log.d("FloatingWindowService", "chatRepository initialized: ${::chatRepository.isInitialized}")

        serviceScope.launch {
            _isLoading.value = true

            // AI 服务会处理用户消息的保存，不要重复保存
            // 调用 AI 服务
            try {
                android.util.Log.d("FloatingWindowService", "Calling aiService.sendMessage...")
                val result = aiService.sendMessage(sessionId, content)
                android.util.Log.d("FloatingWindowService", "aiService.sendMessage result: $result")
                when (result) {
                    is Resource.Success -> {
                        // AI 回复已由 aiService 保存到数据库，Flow 会自动更新 UI
                        // 这里不需要再次保存
                    }
                    is Resource.Error -> {
                        android.util.Log.e("FloatingWindowService", "AI Error: ${result.message}")
                        _events.emit(ChatUiEvent.ShowMessage("发送失败: ${result.message}"))
                    }
                    is Resource.Loading -> {
                        // 已在加载中
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("FloatingWindowService", "Exception: ", e)
                _events.emit(ChatUiEvent.ShowMessage("发送失败: ${e.message}"))
            }

            _isLoading.value = false
        }
    }

    private fun handleEvent(event: ChatUiEvent) {
        when (event) {
            is ChatUiEvent.NavigateTo -> {
                Toast.makeText(
                    this,
                    "准备导航到：${event.targetName ?: event.fallbackQuery ?: "目标地点"}",
                    Toast.LENGTH_SHORT
                ).show()
            }
            is ChatUiEvent.ShowRoute -> {
                Toast.makeText(this, event.summary, Toast.LENGTH_SHORT).show()
            }
            is ChatUiEvent.AskPermission -> {
                Toast.makeText(this, event.reason, Toast.LENGTH_SHORT).show()
            }
            is ChatUiEvent.ShowClarifyQuestion -> {
                Toast.makeText(this, event.question, Toast.LENGTH_SHORT).show()
            }
            is ChatUiEvent.ShowMessage -> {
                Toast.makeText(this, event.message, Toast.LENGTH_SHORT).show()
            }
            is ChatUiEvent.ShowWeather -> {
                Toast.makeText(
                    this,
                    "天气：${event.description}，${event.temperature}°C",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun toggleWindowSize() {
        val targetHeight = if (isExpanded) dpToPx(collapsedHeight) else dpToPx(expandedHeight)
        val currentHeight = floatingParams?.height ?: dpToPx(expandedHeight)

        val animator = android.animation.ValueAnimator.ofInt(currentHeight, targetHeight)
        animator.duration = 200
        animator.addUpdateListener { animation ->
            floatingParams?.let { params ->
                params.height = animation.animatedValue as Int
                windowManager?.updateViewLayout(floatingView, params)
            }
        }
        animator.start()

        isExpanded = !isExpanded

        // 切换图标
        binding.buttonMinimize.setImageResource(
            if (isExpanded) android.R.drawable.arrow_down_float
            else android.R.drawable.arrow_up_float
        )

        // 隐藏/显示内容
        binding.recyclerViewMessages.visibility = if (isExpanded) View.VISIBLE else View.GONE
        binding.inputLayout.visibility = if (isExpanded) View.VISIBLE else View.GONE
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        serviceScope.cancel()

        // 隐藏输入法
        hideInputMethod()

        floatingView?.let {
            try {
                windowManager?.removeView(it)
            } catch (_: Exception) {}
        }
        floatingView = null
        _binding = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "floating_window_channel"
        private const val KEY_SESSION_ID = "chat_session_id"

        fun start(context: Context) {
            val intent = Intent(context, FloatingWindowService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FloatingWindowService::class.java))
        }

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = android.app.NotificationChannel(
                    CHANNEL_ID,
                    "AI 助手",
                    android.app.NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "悬浮窗 AI 助手通知"
                    setShowBadge(false)
                }
                val notificationManager = context.getSystemService(android.app.NotificationManager::class.java)
                notificationManager.createNotificationChannel(channel)
            }
        }
    }
}
