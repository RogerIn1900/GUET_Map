package com.example.guet_map.ui.discover

import androidx.lifecycle.ViewModel
import com.example.guet_map.ui.discover.model.CampusEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class DiscoverSharedViewModel @Inject constructor() : ViewModel() {

    private val _selectedEvent = MutableStateFlow<CampusEvent?>(null)
    val selectedEvent: StateFlow<CampusEvent?> = _selectedEvent.asStateFlow()

    fun setSelectedEvent(event: CampusEvent) {
        _selectedEvent.value = event
    }

    fun clearSelectedEvent() {
        _selectedEvent.value = null
    }
}
