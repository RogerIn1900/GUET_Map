package com.example.guet_map.module.ai.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guet_map.data.UserPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * DeepSeek API Key 配置页 ViewModel
 */
@HiltViewModel
class ApiKeySettingsViewModel @Inject constructor(
    private val userPrefs: UserPrefs
) : ViewModel() {

    private val _currentKey = MutableStateFlow("")
    val currentKey: StateFlow<String> = _currentKey.asStateFlow()

    private val _saveState = MutableSharedFlow<SaveState>()
    val saveState: SharedFlow<SaveState> = _saveState.asSharedFlow()

    init {
        _currentKey.value = userPrefs.deepSeekApiKey
    }

    fun updateKey(key: String) {
        _currentKey.value = key
    }

    fun saveKey(key: String) {
        viewModelScope.launch {
            val trimmed = key.trim()
            when {
                trimmed.isBlank() -> {
                    _saveState.emit(SaveState.Error("API Key 不能为空"))
                }
                !isValidDeepSeekKey(trimmed) -> {
                    _saveState.emit(SaveState.Error("Key 格式不对，DeepSeek API Key 通常以 sk- 开头"))
                }
                else -> {
                    userPrefs.deepSeekApiKey = trimmed
                    _saveState.emit(SaveState.Success)
                }
            }
        }
    }

    fun clearKey() {
        viewModelScope.launch {
            userPrefs.deepSeekApiKey = ""
            _currentKey.value = ""
            _saveState.emit(SaveState.Cleared)
        }
    }

    private fun isValidDeepSeekKey(key: String): Boolean {
        // DeepSeek API Key 通常以 sk- 开头，长度 > 20
        return key.startsWith("sk-") && key.length >= 20
    }
}

sealed class SaveState {
    data object Success : SaveState()
    data object Cleared : SaveState()
    data class Error(val message: String) : SaveState()
}
