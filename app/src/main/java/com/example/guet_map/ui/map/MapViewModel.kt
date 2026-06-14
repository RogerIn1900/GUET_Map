package com.example.guet_map.ui.map

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amap.api.maps.AMap
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.MarkerOptions
import com.example.guet_map.model.GuideStep
import com.example.guet_map.model.Location
import com.example.guet_map.model.Resource
import com.example.guet_map.model.WalkRouteInfo
import com.example.guet_map.repository.LegacyFavoriteRepository
import com.example.guet_map.repository.GuideRepository
import com.example.guet_map.repository.LocationRepository
import com.example.guet_map.ui.map.state.ErrorType
import com.example.guet_map.ui.map.state.MapUiEvent
import com.example.guet_map.ui.map.state.MapUiState
import com.example.guet_map.util.CampusGeo
import com.example.guet_map.util.CampusLocationResolver
import com.example.guet_map.util.CampusSearchMatcher
import com.example.guet_map.util.CampusSearchQueryNormalizer
import com.example.guet_map.util.CampusWalkRoutePlanner
import com.example.guet_map.data.UserPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationRepository: LocationRepository,
    private val guideRepository: GuideRepository,
    private val favoriteRepository: LegacyFavoriteRepository,
    private val walkRoutePlanner: CampusWalkRoutePlanner,
    private val userPrefs: UserPrefs
) : ViewModel() {

    // ============================================================
    // 新的统一状态管理
    // ============================================================

    private val _uiState = MutableStateFlow<MapUiState>(MapUiState.Idle)
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<MapUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    // ============================================================
    // 兼容旧代码的状态（逐步迁移）
    // ============================================================

    private val _walkRoute = MutableStateFlow<WalkRouteInfo?>(null)
    val walkRoute: StateFlow<WalkRouteInfo?> = _walkRoute.asStateFlow()

    private val _routeLoading = MutableStateFlow(false)
    val routeLoading: StateFlow<Boolean> = _routeLoading.asStateFlow()

    private val _routeError = MutableSharedFlow<String>()
    val routeError = _routeError.asSharedFlow()

    init {
        favoriteRepository.switchUser(userPrefs.userId)
        viewModelScope.launch {
            try {
                locationRepository.getLocations().first { it !is Resource.Loading }
                if (userPrefs.isLoggedIn) {
                    favoriteRepository.syncFromServer()
                }
            } catch (_: Exception) {
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "map_privacy"
        private const val KEY_PRIVACY_AGREED = "privacy_agreed"
    }

    // ── 隐私 ─────────────────────────────────────────────────

    val isPrivacyAgreed: Boolean
        get() {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_PRIVACY_AGREED, false)
        }

    fun setPrivacyAgreed() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PRIVACY_AGREED, true)
            .apply()
    }

    // ── 地图 ─────────────────────────────────────────────────

    var aMap: AMap? = null

    // ── 地点数据 ─────────────────────────────────────────────

    /** 缓存的地点列表 (Room → UI 实时同步) */
    val cachedLocations: StateFlow<List<Location>> = locationRepository
        .observeCachedLocations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _locationsResource = MutableStateFlow<Resource<List<Location>>>(Resource.Loading)
    val locationsResource: StateFlow<Resource<List<Location>>> = _locationsResource.asStateFlow()

    /** 当前筛选类别 (null = 全部) */
    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    // ── 图文指引 ─────────────────────────────────────────────

    private val _guideStepsResource = MutableStateFlow<Resource<List<GuideStep>>>(Resource.Loading)
    val guideStepsResource: StateFlow<Resource<List<GuideStep>>> = _guideStepsResource.asStateFlow()

    private val _selectedLocation = MutableStateFlow<Location?>(null)
    val selectedLocation: StateFlow<Location?> = _selectedLocation.asStateFlow()

    val favoriteIds: StateFlow<Set<String>> = favoriteRepository
        .observeFavoriteIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // ── 搜索 ─────────────────────────────────────────────────

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchResults: StateFlow<List<Location>> = _searchQuery
        .combine(cachedLocations) { query, locations ->
            CampusSearchMatcher.filterAndSort(
                locations,
                query,
                limit = CampusSearchQueryNormalizer.MAX_SEARCH_RESULTS
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── 新状态：位置详情 ──────────────────────────────────────

    private val _locationDetailState = MutableStateFlow<LocationDetailState?>(null)
    val locationDetailState: StateFlow<LocationDetailState?> = _locationDetailState.asStateFlow()

    data class LocationDetailState(
        val location: Location,
        val isFavorite: Boolean,
        val guideSteps: List<GuideStep> = emptyList(),
        val isGuideLoading: Boolean = false,
        val errorMessage: String? = null
    )

    // ============================================================
    // 新状态：公开方法
    // ============================================================

    /**
     * 更新 UI 状态
     */
    private fun updateState(state: MapUiState) {
        _uiState.value = state
    }

    /**
     * 发送一次性事件
     */
    private fun sendEvent(event: MapUiEvent) {
        viewModelScope.launch {
            _uiEvent.emit(event)
        }
    }

    /**
     * 显示错误状态
     */
    fun showError(message: String, type: ErrorType = ErrorType.UNKNOWN) {
        updateState(MapUiState.Error(message, type))
    }

    /**
     * 清除错误状态
     */
    fun clearError() {
        val currentState = _uiState.value
        if (currentState is MapUiState.Error) {
            updateState(MapUiState.Idle)
        }
    }

    // ============================================================
    // 搜索相关
    // ============================================================

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearSearchQuery() {
        _searchQuery.value = ""
    }

    fun submitSearch(query: String) {
        val q = query.trim()
        if (q.isEmpty()) return
        _searchQuery.value = q

        updateState(MapUiState.SearchResult(q, searchResults.value))

        val match = resolveSearchLocation(q) ?: return
        pickFromSearch(match)
    }

    /** 搜索选中：定位地图，展开详情卡，清空导航栏，并收起搜索栏 */
    fun pickFromSearch(location: Location) {
        val target = cachedLocations.value.find { it.locationId == location.locationId } ?: location
        _highlightedLocationId.value = target.locationId
        selectLocation(target)
        clearWalkRoute()
        viewModelScope.launch {
            _uiEvent.emit(MapUiEvent.FocusMap(target.latitude, target.longitude))
            _uiEvent.emit(MapUiEvent.DismissSearchInput)
        }
    }

    fun resolveSearchLocation(query: String): Location? =
        CampusLocationResolver.resolveForQuery(query, cachedLocations.value)

    fun focusOnLocation(location: Location) {
        val target = cachedLocations.value.find { it.locationId == location.locationId } ?: location
        _highlightedLocationId.value = target.locationId
        selectLocation(target)
        viewModelScope.launch {
            _uiEvent.emit(MapUiEvent.FocusMap(target.latitude, target.longitude))
        }
    }

    fun updateMapMarkersFromCache() {
        addMarkersForLocations(cachedLocations.value)
    }

    // ============================================================
    // 旧版兼容方法（保留，逐步迁移）
    // ============================================================

    /** 加载所有地点 */
    fun loadLocations() {
        viewModelScope.launch {
            updateState(MapUiState.Loading)
            locationRepository.getLocations().collect { resource ->
                _locationsResource.value = resource
                when (resource) {
                    is Resource.Loading -> updateState(MapUiState.Loading)
                    is Resource.Success -> {
                        updateState(
                            MapUiState.LocationsLoaded(
                                locations = resource.data,
                                filteredLocations = resource.data,
                                selectedCategory = _selectedCategory.value
                            )
                        )
                        addMarkersForLocations(resource.data)
                    }
                    is Resource.Error -> {
                        updateState(
                            MapUiState.Error(
                                message = resource.message,
                                type = ErrorType.LOAD_DATA_FAILED
                            )
                        )
                    }
                }
            }
        }
    }

    /** 按类别筛选 */
    fun filterByCategory(category: String?) {
        _selectedCategory.value = category
        viewModelScope.launch {
            if (category != null) {
                locationRepository.getLocationsByCategory(category).collect { resource ->
                    _locationsResource.value = resource
                    if (resource is Resource.Success) {
                        updateState(
                            MapUiState.LocationsLoaded(
                                locations = cachedLocations.value,
                                filteredLocations = resource.data,
                                selectedCategory = category
                            )
                        )
                        addMarkersForLocations(resource.data)
                    } else if (resource is Resource.Error) {
                        updateState(
                            MapUiState.Error(
                                message = resource.message,
                                type = ErrorType.LOAD_DATA_FAILED
                            )
                        )
                    }
                }
            } else {
                loadLocations()
            }
        }
    }

    /** 加载指定地点的图文指引 */
    fun loadGuideSteps(locationId: String) {
        viewModelScope.launch {
            _locationDetailState.value = _locationDetailState.value?.copy(isGuideLoading = true)
            guideRepository.getGuideSteps(locationId).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _locationDetailState.value = _locationDetailState.value?.copy(isGuideLoading = true)
                    }
                    is Resource.Success -> {
                        _locationDetailState.value = _locationDetailState.value?.copy(
                            guideSteps = resource.data,
                            isGuideLoading = false
                        )
                        _guideStepsResource.value = resource
                    }
                    is Resource.Error -> {
                        _locationDetailState.value = _locationDetailState.value?.copy(
                            isGuideLoading = false,
                            errorMessage = resource.message
                        )
                        _guideStepsResource.value = resource
                    }
                }
            }
        }
    }

    /** 选择地点 → 加载指引并展开 BottomSheet */
    fun selectLocation(location: Location) {
        _selectedLocation.value = location
        val isFav = location.locationId in favoriteIds.value

        _locationDetailState.value = LocationDetailState(
            location = location,
            isFavorite = isFav
        )

        if (location.hasGuide) {
            loadGuideSteps(location.locationId)
        }

        viewModelScope.launch {
            _uiEvent.emit(MapUiEvent.ShowLocationSheet(location.locationId))
        }
    }

    fun selectLocationById(locationId: String) {
        viewModelScope.launch {
            resolveAndSelectLocation(locationId)
        }
    }

    /** 解析地点（内存 → Room → 必要时拉取列表），选中并返回；失败返回 null */
    suspend fun resolveAndSelectLocation(locationId: String): Location? {
        val resolved = resolveLocation(locationId)
        resolved?.let { selectLocation(it) }
        return resolved
    }

    private suspend fun resolveLocation(locationId: String): Location? {
        cachedLocations.value.find { it.locationId == locationId }?.let { return it }
        locationRepository.getCachedLocationById(locationId)?.let { return it }
        if (cachedLocations.value.isEmpty()) {
            locationRepository.getLocations().first { it !is Resource.Loading }
        }
        cachedLocations.value.find { it.locationId == locationId }?.let { return it }
        return locationRepository.getCachedLocationById(locationId)
            ?: favoriteRepository.enrichFavoriteFromCache(locationId)
    }

    suspend fun toggleFavorite(location: Location): Boolean =
        favoriteRepository.toggleFavorite(location)

    fun planWalkRouteTo(destination: Location, start: LatLng) {
        val dest = cachedLocations.value.find { it.locationId == destination.locationId }
            ?: destination
        _routeLoading.value = true
        updateState(
            MapUiState.Navigating(
                target = dest,
                isLoading = true
            )
        )
        walkRoutePlanner.planWalkRoute(
            start = start,
            end = LatLng(dest.latitude, dest.longitude),
            targetName = dest.name,
            onSuccess = { route ->
                _walkRoute.value = route
                _routeLoading.value = false
                updateState(
                    MapUiState.Navigating(
                        target = dest,
                        route = route,
                        isLoading = false
                    )
                )
            },
            onError = { message ->
                _routeLoading.value = false
                updateState(
                    MapUiState.Navigating(
                        target = dest,
                        isLoading = false,
                        errorMessage = message
                    )
                )
                viewModelScope.launch {
                    _uiEvent.emit(MapUiEvent.ShowToast(message))
                }
            }
        )
    }

    fun clearWalkRoute() {
        _walkRoute.value = null
        if (_uiState.value is MapUiState.Navigating) {
            updateState(MapUiState.Idle)
        }
    }

    /** 花江校区中心（无 GPS 时的默认起点） */
    fun campusCenterLatLng(): LatLng = LatLng(CampusGeo.CENTER_LAT, CampusGeo.CENTER_LNG)

    // ── Marker 管理 ──────────────────────────────────────────

    private var addedMarkers: List<com.amap.api.maps.model.Marker> = emptyList()
    private var highlightMarker: com.amap.api.maps.model.Marker? = null

    private val _highlightedLocationId = MutableStateFlow<String?>(null)
    val highlightedLocationId: StateFlow<String?> = _highlightedLocationId.asStateFlow()

    private fun addMarkersForLocations(locations: List<Location>) {
        val map = aMap ?: return
        addedMarkers.forEach { it.remove() }
        highlightMarker?.remove()
        highlightMarker = null

        addedMarkers = locations.map { loc ->
            val marker = map.addMarker(
                MarkerOptions()
                    .position(LatLng(loc.latitude, loc.longitude))
                    .title(loc.name)
                    .snippet(loc.category)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            )
            marker.`object` = loc
            marker
        }

        _highlightedLocationId.value?.let { id ->
            locations.find { it.locationId == id }?.let { showHighlightMarker(it) }
        }
    }

    fun showHighlightMarker(location: Location) {
        val map = aMap ?: return
        highlightMarker?.remove()
        highlightMarker = map.addMarker(
            MarkerOptions()
                .position(LatLng(location.latitude, location.longitude))
                .title(location.name)
                .snippet("已选中")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                .zIndex(2f)
        )
        highlightMarker?.`object` = location
    }

    override fun onCleared() {
        super.onCleared()
        aMap = null
    }
}
