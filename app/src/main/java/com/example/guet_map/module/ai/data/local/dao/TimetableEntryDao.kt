package com.example.guet_map.module.ai.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.guet_map.module.ai.data.local.entity.TimetableEntryEntity
import kotlinx.coroutines.flow.Flow

/**
 * AI 模块课表 DAO
 */
@Dao
interface TimetableEntryDao {

    @Query("SELECT * FROM timetable_entries WHERE userId = :userId AND semester = :semester ORDER BY dayOfWeek ASC, startPeriod ASC")
    fun observeEntries(userId: String, semester: String): Flow<List<TimetableEntryEntity>>

    @Query("SELECT * FROM timetable_entries WHERE userId = :userId AND semester = :semester AND dayOfWeek = :dayOfWeek ORDER BY startPeriod ASC")
    fun observeEntriesForDay(userId: String, semester: String, dayOfWeek: Int): Flow<List<TimetableEntryEntity>>

    @Query("SELECT * FROM timetable_entries WHERE userId = :userId AND semester = :semester AND dayOfWeek = :dayOfWeek AND startPeriod <= :period AND endPeriod >= :period LIMIT 1")
    suspend fun findEntryAt(userId: String, semester: String, dayOfWeek: Int, period: Int): TimetableEntryEntity?

    @Query("SELECT * FROM timetable_entries WHERE userId = :userId AND semester = :semester AND dayOfWeek = :dayOfWeek AND startPeriod >= :period LIMIT 1")
    suspend fun findNextEntry(userId: String, semester: String, dayOfWeek: Int, period: Int): TimetableEntryEntity?

    @Query("SELECT * FROM timetable_entries WHERE userId = :userId AND semester = :semester")
    suspend fun getEntriesForSemester(userId: String, semester: String): List<TimetableEntryEntity>

    @Query("DELETE FROM timetable_entries WHERE userId = :userId AND semester = :semester")
    suspend fun deleteBySemester(userId: String, semester: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<TimetableEntryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: TimetableEntryEntity)

    @Delete
    suspend fun delete(entry: TimetableEntryEntity)

    @Query("DELETE FROM timetable_entries")
    suspend fun deleteAll()
}
