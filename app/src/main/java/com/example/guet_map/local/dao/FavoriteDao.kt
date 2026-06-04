package com.example.guet_map.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.guet_map.local.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Query("SELECT * FROM favorites ORDER BY added_at DESC")
    fun observeAll(): Flow<List<FavoriteEntity>>

    @Query("SELECT location_id FROM favorites")
    fun observeFavoriteIds(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM favorites WHERE location_id = :locationId")
    suspend fun isFavorite(locationId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE location_id = :locationId")
    suspend fun delete(locationId: String)

    @Query("DELETE FROM favorites")
    suspend fun deleteAll()
}
