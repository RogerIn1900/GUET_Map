package com.example.guet_map.ui

import com.example.guet_map.R
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class MainNavViewModel @Inject constructor() : ViewModel() {

    private val _pendingLocationId = MutableStateFlow<String?>(null)
    val pendingLocationId: StateFlow<String?> = _pendingLocationId.asStateFlow()

    private val _selectedTab = MutableStateFlow<Int?>(null)
    val selectedTab: StateFlow<Int?> = _selectedTab.asStateFlow()

    private val _pendingCategory = MutableStateFlow<String?>(null)
    val pendingCategory: StateFlow<String?> = _pendingCategory.asStateFlow()

    fun openLocationOnMap(locationId: String) {
        // #region agent log
        com.example.guet_map.util.AgentDebugLog.log(
            "H2",
            "MainNavViewModel.openLocationOnMap",
            "pending set",
            mapOf("locationId" to locationId)
        )
        // #endregion
        _pendingLocationId.value = locationId
        _selectedTab.value = R.id.nav_map
    }

    fun openMapWithCategory(category: String) {
        _pendingCategory.value = category
        _selectedTab.value = R.id.nav_map
    }

    fun consumePendingCategory(): String? {
        val category = _pendingCategory.value
        _pendingCategory.value = null
        return category
    }

    fun consumePendingLocation(): String? {
        val id = _pendingLocationId.value
        _pendingLocationId.value = null
        return id
    }

    fun requestTab(tabId: Int) {
        _selectedTab.value = tabId
    }

    private val _pendingWeatherDetail = MutableStateFlow(false)
    val pendingWeatherDetail: StateFlow<Boolean> = _pendingWeatherDetail.asStateFlow()

    fun requestWeatherDetail() {
        _selectedTab.value = R.id.nav_map
        _pendingWeatherDetail.value = true
    }

    fun consumeWeatherDetailRequest(): Boolean {
        val pending = _pendingWeatherDetail.value
        _pendingWeatherDetail.value = false
        return pending
    }

    fun consumeTabRequest(): Int? {
        val tab = _selectedTab.value
        _selectedTab.value = null
        return tab
    }
}
