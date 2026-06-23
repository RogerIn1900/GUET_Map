package com.example.guet_map.module.ai.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guet_map.module.ai.data.model.TimetableEntry
import com.example.guet_map.module.ai.data.repository.TimetableRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class TimetableImportViewModel @Inject constructor(
    private val timetableRepository: TimetableRepository
) : ViewModel() {

    private val _semester = MutableStateFlow(getCurrentSemester())
    val semester: StateFlow<String> = _semester.asStateFlow()

    private val _entries = MutableStateFlow<List<TimetableEntry>>(emptyList())
    val entries: StateFlow<List<TimetableEntry>> = _entries.asStateFlow()

    private val _events = MutableSharedFlow<ImportEvent>()
    val events: SharedFlow<ImportEvent> = _events.asSharedFlow()

    private var loadJob: Job? = null

    init {
        loadEntries()
    }

    private fun loadEntries() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            timetableRepository.observeEntries(_semester.value).collect { list ->
                _entries.value = list
            }
        }
    }

    fun setSemester(semester: String) {
        if (_semester.value != semester) {
            _semester.value = semester
            loadEntries()
        }
    }

    fun saveEntry(
        courseName: String,
        classroomName: String,
        dayOfWeek: Int,
        startPeriod: Int,
        endPeriod: Int,
        weekRange: String,
        semester: String
    ) {
        if (courseName.isBlank()) {
            viewModelScope.launch { _events.emit(ImportEvent.ValidationError("课程名称不能为空")) }
            return
        }
        if (classroomName.isBlank()) {
            viewModelScope.launch { _events.emit(ImportEvent.ValidationError("教室不能为空")) }
            return
        }
        if (startPeriod > endPeriod) {
            viewModelScope.launch { _events.emit(ImportEvent.ValidationError("开始节次不能大于结束节次")) }
            return
        }

        viewModelScope.launch {
            val entry = TimetableEntry(
                id = UUID.randomUUID().toString(),
                userId = "",
                courseName = courseName.trim(),
                teacherName = null,
                classroomName = classroomName.trim(),
                locationId = null,
                dayOfWeek = dayOfWeek,
                startPeriod = startPeriod,
                endPeriod = endPeriod,
                weekRange = weekRange.trim().ifBlank { "1-16" },
                semester = semester,
                latitude = null,
                longitude = null
            )
            timetableRepository.saveEntry(entry)
            _events.emit(ImportEvent.SaveSuccess)
        }
    }

    fun deleteEntry(entry: TimetableEntry) {
        viewModelScope.launch {
            timetableRepository.deleteEntry(entry)
        }
    }

    fun updateEntry(
        entry: TimetableEntry,
        courseName: String,
        classroomName: String,
        dayOfWeek: Int,
        startPeriod: Int,
        endPeriod: Int,
        weekRange: String
    ) {
        viewModelScope.launch {
            val updatedEntry = entry.copy(
                courseName = courseName,
                classroomName = classroomName,
                dayOfWeek = dayOfWeek,
                startPeriod = startPeriod,
                endPeriod = endPeriod,
                weekRange = weekRange
            )
            timetableRepository.saveEntry(updatedEntry)
        }
    }

    private fun getCurrentSemester(): String {
        val cal = java.util.Calendar.getInstance()
        val year = cal.get(java.util.Calendar.YEAR)
        val month = cal.get(java.util.Calendar.MONTH) + 1
        val term = if (month in 2..7) 2 else 1
        return "$year-$term"
    }

    sealed class ImportEvent {
        data object SaveSuccess : ImportEvent()
        data class ValidationError(val message: String) : ImportEvent()
    }
}
