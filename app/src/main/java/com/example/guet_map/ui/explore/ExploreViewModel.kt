package com.example.guet_map.ui.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guet_map.model.Location
import com.example.guet_map.model.RecentGuide
import com.example.guet_map.model.Resource
import com.example.guet_map.repository.ExploreRepository
import com.example.guet_map.repository.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val exploreRepository: ExploreRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {

    val hotLocations: StateFlow<List<Location>> = exploreRepository
        .getHotLocations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _recentGuides = MutableStateFlow<Resource<List<RecentGuide>>>(Resource.Loading)
    val recentGuides: StateFlow<Resource<List<RecentGuide>>> = _recentGuides.asStateFlow()

    val categories = listOf(
        "食堂", "教室", "咖啡", "图书馆", "宿舍", "校门", "商店", "运动场"
    )

    init {
        viewModelScope.launch {
            locationRepository.getLocations().collect { /* warm cache */ }
        }
        loadRecentGuides()
    }

    fun loadRecentGuides() {
        viewModelScope.launch {
            exploreRepository.getRecentGuides().collect { _recentGuides.value = it }
        }
    }

    fun filterByCategory(category: String) {
        viewModelScope.launch {
            locationRepository.getLocationsByCategory(category).collect { /* cache */ }
        }
    }
}
