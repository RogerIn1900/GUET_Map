package com.example.guet_map.module.ai.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.guet_map.databinding.FragmentChatBinding
import com.example.guet_map.module.ai.ui.bubble.FloatingBubbleService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * AI 对话界面
 */
@AndroidEntryPoint
class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatViewModel by viewModels()

    private lateinit var messageAdapter: ChatMessageAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSendButton()
        setupFloatingBubbleButton()
        setupSettingsButton()
        observeState()
    }

    private fun setupRecyclerView() {
        messageAdapter = ChatMessageAdapter()
        binding.recyclerViewMessages.apply {
            adapter = messageAdapter
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true
            }
        }
    }

    private fun setupSendButton() {
        binding.buttonSend.setOnClickListener {
            val message = binding.editTextMessage.text.toString()
            if (message.isNotBlank()) {
                viewModel.sendMessage(message)
                binding.editTextMessage.text?.clear()
            }
        }
    }

    private fun setupFloatingBubbleButton() {
        binding.buttonFloatingBubble.setOnClickListener {
            if (hasOverlayPermission()) {
                FloatingBubbleService.show(requireContext())
                Toast.makeText(requireContext(), "悬浮球已开启", Toast.LENGTH_SHORT).show()
            } else {
                requestOverlayPermission()
            }
        }
    }

    private fun setupSettingsButton() {
        binding.buttonSettings.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(
                    com.example.guet_map.R.id.navHostFragment,
                    com.example.guet_map.module.ai.ui.settings.ApiKeySettingsFragment.newInstance()
                )
                .addToBackStack(null)
                .commit()
        }
    }

    private fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.provider.Settings.canDrawOverlays(requireContext())
        } else {
            true
        }
    }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (hasOverlayPermission()) {
            FloatingBubbleService.show(requireContext())
            Toast.makeText(requireContext(), "悬浮球已开启", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = android.content.Intent(
                android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:${requireContext().packageName}")
            )
            overlayPermissionLauncher.launch(intent)
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.messages.collect { messages ->
                        messageAdapter.submitList(messages)
                        if (messages.isNotEmpty()) {
                            binding.recyclerViewMessages.scrollToPosition(messages.size - 1)
                        }
                    }
                }

                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                        binding.buttonSend.isEnabled = !isLoading
                    }
                }

                launch {
                    viewModel.events.collect { event ->
                        handleEvent(event)
                    }
                }
            }
        }
    }

    private fun handleEvent(event: ChatUiEvent) {
        when (event) {
            is ChatUiEvent.NavigateTo -> {
                setNavigationResult(event)
                Toast.makeText(
                    requireContext(),
                    "准备导航到：${event.targetName ?: event.fallbackQuery ?: "目标地点"}",
                    Toast.LENGTH_SHORT
                ).show()
            }

            is ChatUiEvent.ShowRoute -> {
                setRouteResult(event)
                Toast.makeText(
                    requireContext(),
                    event.summary,
                    Toast.LENGTH_SHORT
                ).show()
            }

            is ChatUiEvent.AskPermission -> {
                setPermissionResult(event)
                Toast.makeText(
                    requireContext(),
                    event.reason,
                    Toast.LENGTH_SHORT
                ).show()
            }

            is ChatUiEvent.ShowClarifyQuestion -> {
                setClarifyResult(event)
                Toast.makeText(
                    requireContext(),
                    event.question,
                    Toast.LENGTH_SHORT
                ).show()
            }

            is ChatUiEvent.ShowMessage -> {
                Toast.makeText(
                    requireContext(),
                    event.message,
                    Toast.LENGTH_SHORT
                ).show()
            }

            is ChatUiEvent.ShowWeather -> {
                handleWeatherEvent(event)
            }
        }
    }

    private fun handleWeatherEvent(event: ChatUiEvent.ShowWeather) {
        setWeatherResult(event)
        Toast.makeText(
            requireContext(),
            "天气：${event.description}，${event.temperature}°C",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun setWeatherResult(event: ChatUiEvent.ShowWeather) {
        parentFragmentManager.setFragmentResult(
            REQUEST_KEY_AI_ACTION,
            Bundle().apply {
                putString(KEY_ACTION_TYPE, ACTION_SHOW_WEATHER)
                putString(KEY_WEATHER_SUMMARY, event.summary)
                putInt(KEY_WEATHER_TEMPERATURE, event.temperature)
                putString(KEY_WEATHER_DESCRIPTION, event.description)
                putInt(KEY_WEATHER_FEELS_LIKE, event.feelsLike)
                putInt(KEY_WEATHER_HUMIDITY, event.humidity)
                putFloat(KEY_WEATHER_WIND_SPEED, event.windSpeed)
                putString(KEY_WEATHER_WIND_DIRECTION, event.windDirection)
                event.aqi?.let { putInt(KEY_WEATHER_AQI, it) }
                putString(KEY_WEATHER_AQI_LEVEL, event.aqiLevel)
                event.uvIndex?.let { putInt(KEY_WEATHER_UV_INDEX, it) }
                putString(KEY_WEATHER_ALERT, event.alertMessage)
                putString(KEY_WEATHER_SAFETY_TIPS, event.safetyTips)
            }
        )
    }

    private fun setNavigationResult(event: ChatUiEvent.NavigateTo) {
        parentFragmentManager.setFragmentResult(
            REQUEST_KEY_AI_ACTION,
            Bundle().apply {
                putString(KEY_ACTION_TYPE, ACTION_NAVIGATE_TO)
                putString(KEY_TARGET_NAME, event.targetName)
                putString(KEY_TARGET_LOCATION_ID, event.targetLocationId)
                putString(KEY_FALLBACK_QUERY, event.fallbackQuery)
                putString(KEY_MODE, event.mode)
            }
        )
    }

    private fun setRouteResult(event: ChatUiEvent.ShowRoute) {
        parentFragmentManager.setFragmentResult(
            REQUEST_KEY_AI_ACTION,
            Bundle().apply {
                putString(KEY_ACTION_TYPE, ACTION_SHOW_ROUTE)
                putString(KEY_ROUTE_SUMMARY, event.summary)
                putInt(KEY_ROUTE_DISTANCE, event.distance ?: -1)
                putInt(KEY_ROUTE_DURATION, event.durationMin ?: -1)
            }
        )
    }

    private fun setPermissionResult(event: ChatUiEvent.AskPermission) {
        parentFragmentManager.setFragmentResult(
            REQUEST_KEY_AI_ACTION,
            Bundle().apply {
                putString(KEY_ACTION_TYPE, ACTION_ASK_PERMISSION)
                putString(KEY_PERMISSION, event.permission)
                putString(KEY_REASON, event.reason)
            }
        )
    }

    private fun setClarifyResult(event: ChatUiEvent.ShowClarifyQuestion) {
        parentFragmentManager.setFragmentResult(
            REQUEST_KEY_AI_ACTION,
            Bundle().apply {
                putString(KEY_ACTION_TYPE, ACTION_CLARIFY)
                putString(KEY_QUESTION, event.question)
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val REQUEST_KEY_AI_ACTION = "guet_map_ai_action"

        const val ACTION_NAVIGATE_TO = "navigate_to"
        const val ACTION_SHOW_ROUTE = "show_route"
        const val ACTION_ASK_PERMISSION = "ask_permission"
        const val ACTION_CLARIFY = "clarify"
        const val ACTION_SHOW_WEATHER = "show_weather"

        const val KEY_ACTION_TYPE = "action_type"
        const val KEY_TARGET_NAME = "target_name"
        const val KEY_TARGET_LOCATION_ID = "target_location_id"
        const val KEY_FALLBACK_QUERY = "fallback_query"
        const val KEY_MODE = "mode"
        const val KEY_ROUTE_SUMMARY = "route_summary"
        const val KEY_ROUTE_DISTANCE = "route_distance"
        const val KEY_ROUTE_DURATION = "route_duration"
        const val KEY_PERMISSION = "permission"
        const val KEY_REASON = "reason"
        const val KEY_QUESTION = "question"
        const val KEY_WEATHER_SUMMARY = "weather_summary"
        const val KEY_WEATHER_TEMPERATURE = "weather_temperature"
        const val KEY_WEATHER_DESCRIPTION = "weather_description"
        const val KEY_WEATHER_FEELS_LIKE = "weather_feels_like"
        const val KEY_WEATHER_HUMIDITY = "weather_humidity"
        const val KEY_WEATHER_WIND_SPEED = "weather_wind_speed"
        const val KEY_WEATHER_WIND_DIRECTION = "weather_wind_direction"
        const val KEY_WEATHER_AQI = "weather_aqi"
        const val KEY_WEATHER_AQI_LEVEL = "weather_aqi_level"
        const val KEY_WEATHER_UV_INDEX = "weather_uv_index"
        const val KEY_WEATHER_ALERT = "weather_alert"
        const val KEY_WEATHER_SAFETY_TIPS = "weather_safety_tips"

        fun newInstance(locationId: String? = null) = ChatFragment().apply {
            arguments = Bundle().apply {
                locationId?.let { putString("locationId", it) }
            }
        }
    }
}
