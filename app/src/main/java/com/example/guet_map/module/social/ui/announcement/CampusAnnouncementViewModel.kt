package com.example.guet_map.module.social.ui.announcement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guet_map.module.social.data.model.AnnouncementCategory
import com.example.guet_map.module.social.data.model.CampusAnnouncement
import com.example.guet_map.module.social.domain.usecase.GetCampusAnnouncementsUseCase
import com.example.guet_map.module.social.domain.usecase.MarkAnnouncementReadUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CampusAnnouncementViewModel @Inject constructor(
    private val getAnnouncementsUseCase: GetCampusAnnouncementsUseCase,
    private val markReadUseCase: MarkAnnouncementReadUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<AnnouncementUiState>(AnnouncementUiState.Loading)
    val uiState: StateFlow<AnnouncementUiState> = _uiState.asStateFlow()

    private val _announcements = MutableStateFlow<List<CampusAnnouncement>>(emptyList())
    val announcements: StateFlow<List<CampusAnnouncement>> = _announcements.asStateFlow()

    private val _selectedCategory = MutableStateFlow<AnnouncementCategory?>(null)
    val selectedCategory: StateFlow<AnnouncementCategory?> = _selectedCategory.asStateFlow()

    private val _expandedId = MutableStateFlow<String?>(null)
    val expandedId: StateFlow<String?> = _expandedId.asStateFlow()

    init {
        loadAnnouncements()
    }

    private fun loadAnnouncements() {
        viewModelScope.launch {
            getAnnouncementsUseCase.seedIfNeeded()

            _selectedCategory.collectLatest { category ->
                val flow = if (category != null) {
                    getAnnouncementsUseCase.byCategory(category)
                } else {
                    getAnnouncementsUseCase()
                }

                flow.collect { list ->
                    _announcements.value = list
                    _uiState.value = if (list.isEmpty()) {
                        AnnouncementUiState.Empty
                    } else {
                        AnnouncementUiState.Success(list)
                    }
                }
            }
        }
    }

    fun selectCategory(category: AnnouncementCategory?) {
        _selectedCategory.value = category
    }

    fun toggleExpand(id: String) {
        val previous = _expandedId.value
        _expandedId.value = if (previous == id) null else id

        if (previous != id) {
            viewModelScope.launch {
                markReadUseCase(id)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = AnnouncementUiState.Refreshing
            getAnnouncementsUseCase.seedIfNeeded()
            _uiState.value = if (_announcements.value.isEmpty()) {
                AnnouncementUiState.Empty
            } else {
                AnnouncementUiState.Success(_announcements.value)
            }
        }
    }
}

sealed class AnnouncementUiState {
    data object Loading : AnnouncementUiState()
    data object Refreshing : AnnouncementUiState()
    data object Empty : AnnouncementUiState()
    data class Success(val announcements: List<CampusAnnouncement>) : AnnouncementUiState()
    data class Error(val message: String) : AnnouncementUiState()
}
