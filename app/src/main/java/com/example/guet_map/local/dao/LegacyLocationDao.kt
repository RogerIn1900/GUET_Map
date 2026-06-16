package com.example.guet_map.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.guet_map.local.entity.LegacyLocationEntity
import kotlinx.coroutines.flow.Flow

/**
 * 地点 DAO（旧版兼容）
 * 表名: legacy_locations
 */
@Dao
interface LegacyLocationDao {

    @Query("SELECT * FROM legacy_locations")
    fun getAllLocations(): Flow<List<LegacyLocationEntity>>

    @Query("SELECT * FROM legacy_locations WHERE category = :category")
    fun getLocationsByCategory(category: String): Flow<List<LegacyLocationEntity>>

    @Query("SELECT * FROM legacy_locations WHERE location_id = :locationId")
    suspend fun getLocationById(locationId: String): LegacyLocationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(locations: List<LegacyLocationEntity>)

    @Query("DELETE FROM legacy_locations")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM legacy_locations")
    suspend fun count(): Int
}
