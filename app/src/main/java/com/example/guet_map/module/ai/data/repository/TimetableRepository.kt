package com.example.guet_map.module.ai.data.repository

import com.example.guet_map.data.UserPrefs
import com.example.guet_map.module.ai.data.local.dao.TimetableEntryDao
import com.example.guet_map.module.ai.data.local.entity.TimetableEntryEntity
import com.example.guet_map.module.ai.data.model.TimetableEntry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimetableRepository @Inject constructor(
    private val timetableEntryDao: TimetableEntryDao,
    private val userPrefs: UserPrefs
) {
    private val activeUserId = MutableStateFlow(currentUserId())

    init {
        activeUserId.value = currentUserId()
    }

    private fun currentUserId(): String =
        userPrefs.userId.ifBlank { UserPrefs.GUEST_USER_ID }

    fun refreshUserId() {
        activeUserId.value = currentUserId()
    }

    fun switchUser(userId: String) {
        activeUserId.value = userId.ifBlank { UserPrefs.GUEST_USER_ID }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeEntries(semester: String): Flow<List<TimetableEntry>> {
        return activeUserId.flatMapLatest { uid ->
            timetableEntryDao.observeEntries(uid, semester).map { entities ->
                entities.map { it.toDomain() }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeEntriesForDay(semester: String, dayOfWeek: Int): Flow<List<TimetableEntry>> {
        return activeUserId.flatMapLatest { uid ->
            timetableEntryDao.observeEntriesForDay(uid, semester, dayOfWeek).map { entities ->
                entities.map { it.toDomain() }
            }
        }
    }

    suspend fun getCurrentEntry(semester: String): TimetableEntry? {
        val uid = activeUserId.value
        val now = java.util.Calendar.getInstance()
        val dayOfWeek = now.get(java.util.Calendar.DAY_OF_WEEK)
        val adjustedDayOfWeek = when (dayOfWeek) {
            java.util.Calendar.SUNDAY -> 7
            else -> dayOfWeek - 1
        }
        val period = currentPeriod()
        return if (period != null) {
            timetableEntryDao.findEntryAt(uid, semester, adjustedDayOfWeek, period)?.toDomain()
                ?: timetableEntryDao.findNextEntry(uid, semester, adjustedDayOfWeek, period)?.toDomain()
        } else {
            timetableEntryDao.findNextEntry(uid, semester, adjustedDayOfWeek, 1)?.toDomain()
        }
    }

    suspend fun getTodayEntries(semester: String): List<TimetableEntry> {
        val uid = activeUserId.value
        val now = java.util.Calendar.getInstance()
        val dayOfWeek = now.get(java.util.Calendar.DAY_OF_WEEK)
        val adjustedDayOfWeek = when (dayOfWeek) {
            java.util.Calendar.SUNDAY -> 7
            else -> dayOfWeek - 1
        }
        val entries = timetableEntryDao.getEntriesForSemester(uid, semester)
        return entries.filter { it.dayOfWeek == adjustedDayOfWeek }.map { it.toDomain() }
    }

    suspend fun saveEntry(entry: TimetableEntry) {
        val uid = activeUserId.value
        val entity = entry.toEntity(uid)
        timetableEntryDao.insert(entity)
    }

    suspend fun saveEntries(entries: List<TimetableEntry>, semester: String) {
        val uid = activeUserId.value
        val entities = entries.map { entry ->
            TimetableEntryEntity(
                id = entry.id,
                userId = uid,
                courseName = entry.courseName,
                teacherName = entry.teacherName,
                classroomName = entry.classroomName,
                locationId = entry.locationId,
                dayOfWeek = entry.dayOfWeek,
                startPeriod = entry.startPeriod,
                endPeriod = entry.endPeriod,
                weekRange = entry.weekRange,
                semester = semester,
                latitude = entry.latitude,
                longitude = entry.longitude,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        }
        timetableEntryDao.insertAll(entities)
    }

    suspend fun deleteEntry(entry: TimetableEntry) {
        val uid = activeUserId.value
        val entity = TimetableEntryEntity(
            id = entry.id,
            userId = uid,
            courseName = entry.courseName,
            teacherName = entry.teacherName,
            classroomName = entry.classroomName,
            locationId = entry.locationId,
            dayOfWeek = entry.dayOfWeek,
            startPeriod = entry.startPeriod,
            endPeriod = entry.endPeriod,
            weekRange = entry.weekRange,
            semester = entry.semester,
            latitude = entry.latitude,
            longitude = entry.longitude,
            createdAt = 0L,
            updatedAt = System.currentTimeMillis()
        )
        timetableEntryDao.delete(entity)
    }

    suspend fun clearSemester(semester: String) {
        val uid = activeUserId.value
        timetableEntryDao.deleteBySemester(uid, semester)
    }

    private fun currentPeriod(): Int? {
        val now = java.util.Calendar.getInstance()
        val hour = now.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = now.get(java.util.Calendar.MINUTE)
        val totalMinutes = hour * 60 + minute

        return when {
            totalMinutes in 480..525 -> 1
            totalMinutes in 525..585 -> 2
            totalMinutes in 585..645 -> 3
            totalMinutes in 645..690 -> 4
            totalMinutes in 690..735 -> 5
            totalMinutes in 840..885 -> 6
            totalMinutes in 885..945 -> 7
            totalMinutes in 945..1005 -> 8
            totalMinutes in 1005..1050 -> 9
            totalMinutes in 1050..1095 -> 10
            totalMinutes in 1170..1215 -> 11
            totalMinutes in 1215..1260 -> 12
            totalMinutes in 1260..1305 -> 13
            totalMinutes in 1305..1350 -> 14
            else -> null
        }
    }

    private fun TimetableEntryEntity.toDomain(): TimetableEntry {
        return TimetableEntry(
            id = id,
            userId = userId,
            courseName = courseName,
            teacherName = teacherName,
            classroomName = classroomName,
            locationId = locationId,
            dayOfWeek = dayOfWeek,
            startPeriod = startPeriod,
            endPeriod = endPeriod,
            weekRange = weekRange,
            semester = semester,
            latitude = latitude,
            longitude = longitude
        )
    }

    private fun TimetableEntry.toEntity(overrideUserId: String): TimetableEntryEntity {
        return TimetableEntryEntity(
            id = id,
            userId = overrideUserId,
            courseName = courseName,
            teacherName = teacherName,
            classroomName = classroomName,
            locationId = locationId,
            dayOfWeek = dayOfWeek,
            startPeriod = startPeriod,
            endPeriod = endPeriod,
            weekRange = weekRange,
            semester = semester,
            latitude = latitude,
            longitude = longitude,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }
}
