package com.example.guet_map

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.guet_map.databinding.ActivityMainBinding
import com.example.guet_map.module.ai.ui.chat.ChatFragment
import com.example.guet_map.ui.MainNavViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val mainNavViewModel: MainNavViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // #region agent log
        com.example.guet_map.util.AgentDebugLog.log(
            "S3", "MainActivity.onCreate", "start",
            emptyMap(), runId = "crash-fix"
        )
        // #endregion
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyWindowInsets()
        setupNavigation()
        setupAiActionListener()

        // 处理从悬浮球打开 AI 聊天
        if (intent.getBooleanExtra("open_ai_chat", false)) {
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    mainNavViewModel.requestTab(R.id.nav_notifications)
                }
            }
        }
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController

        // 防止资源合并或运行时重复注入导致底部导航菜单项超过 5 个而崩溃
        while (binding.bottomNavigation.menu.size() > 5) {
            binding.bottomNavigation.menu.removeItem(binding.bottomNavigation.menu.getItem(binding.bottomNavigation.menu.size() - 1).itemId)
        }
        binding.bottomNavigation.setupWithNavController(navController)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainNavViewModel.selectedTab.collectLatest { tabId ->
                    tabId ?: return@collectLatest
                    val consumed = mainNavViewModel.consumeTabRequest()
                    if (consumed != null && navController.currentDestination?.id != consumed) {
                        val menu = binding.bottomNavigation.menu
                        if (menu.findItem(consumed) != null) {
                            binding.bottomNavigation.selectedItemId = consumed
                        } else {
                            try {
                                navController.navigate(consumed)
                            } catch (_: Exception) {
                                // 如果导航失败（目的地可能不存在于 navGraph），忽略以避免二次崩溃
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setupAiActionListener() {
        supportFragmentManager.setFragmentResultListener(
            ChatFragment.REQUEST_KEY_AI_ACTION,
            this
        ) { _, bundle ->
            handleAiAction(bundle)
        }
    }

    private fun handleAiAction(bundle: Bundle) {
        val actionType = bundle.getString(ChatFragment.KEY_ACTION_TYPE) ?: return

        when (actionType) {
            ChatFragment.ACTION_NAVIGATE_TO -> {
                val targetName = bundle.getString(ChatFragment.KEY_TARGET_NAME)
                val targetLocationId = bundle.getString(ChatFragment.KEY_TARGET_LOCATION_ID)
                val fallbackQuery = bundle.getString(ChatFragment.KEY_FALLBACK_QUERY)
                handleNavigateTo(targetName, targetLocationId, fallbackQuery)
            }
            ChatFragment.ACTION_SHOW_ROUTE -> {
                val summary = bundle.getString(ChatFragment.KEY_ROUTE_SUMMARY)
                Toast.makeText(this, summary ?: "路线已规划", Toast.LENGTH_SHORT).show()
            }
            ChatFragment.ACTION_SHOW_WEATHER -> {
                val description = bundle.getString(ChatFragment.KEY_WEATHER_DESCRIPTION)
                val temperature = bundle.getInt(ChatFragment.KEY_WEATHER_TEMPERATURE, 0)
                Toast.makeText(
                    this,
                    "天气：$description，${temperature}°C",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun handleNavigateTo(targetName: String?, targetLocationId: String?, fallbackQuery: String?) {
        // 传递位置 ID 给 MapFragment（同时切换到地图 Tab）
        val locationId = targetLocationId ?: fallbackQuery
        if (locationId != null) {
            mainNavViewModel.openLocationOnMap(locationId)
            Toast.makeText(
                this,
                "正在导航到：${targetName ?: locationId}",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            // 没有位置 ID 时，尝试用名称搜索
            val searchQuery = targetName ?: fallbackQuery
            if (searchQuery != null) {
                mainNavViewModel.openLocationOnMap(searchQuery)
                Toast.makeText(
                    this,
                    "正在搜索：$searchQuery",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this,
                    "无法确定目标位置",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.navHostFragment.setPadding(0, systemBars.top, 0, 0)
            binding.bottomNavigation.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }
    }
}
