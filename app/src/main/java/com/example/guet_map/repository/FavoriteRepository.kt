package com.example.guet_map.repository

import com.example.guet_map.local.dao.FavoriteDao
import com.example.guet_map.local.dao.LocationDao
import com.example.guet_map.local.entity.FavoriteEntity
import com.example.guet_map.model.FavoriteRequest
import com.example.guet_map.model.Location
import com.example.guet_map.network.ApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoriteRepository @Inject constructor(
    private val apiService: ApiService,
    private val favoriteDao: FavoriteDao,
    private val locationDao: LocationDao
) {

    fun observeFavoriteIds(): Flow<Set<String>> =
        favoriteDao.observeFavoriteIds().map { it.toSet() }

    fun observeFavoriteLocations(): Flow<List<Location>> =
        combine(
            favoriteDao.observeAll(),
            locationDao.getAllLocations()
        ) { favorites, locations ->
            val idOrder = favorites.map { it.locationId }
            val byId = locations.associateBy { it.locationId }
            idOrder.mapNotNull { id ->
                byId[id]?.let { entity ->
                    Location(
                        locationId = entity.locationId,
                        name = entity.name,
                        latitude = entity.latitude,
                        longitude = entity.longitude,
                        category = entity.category,
                        rating = entity.rating,
                        openingHours = entity.openingHours,
                        imageUrl = entity.imageUrl,
                        hasGuide = entity.hasGuide
                    )
                }
            }
        }

    suspend fun isFavorite(locationId: String): Boolean =
        favoriteDao.isFavorite(locationId) > 0

    suspend fun toggleFavorite(location: Location): Boolean {
        val currentlyFavorite = isFavorite(location.locationId)
        return if (currentlyFavorite) {
            removeFavorite(location.locationId)
            false
        } else {
            addFavorite(location)
            true
        }
    }

    suspend fun addFavorite(location: Location) {
        try {
            apiService.addFavorite(FavoriteRequest(location.locationId))
        } catch (_: Exception) {
            // 离线时仅写本地
        }
        favoriteDao.insert(FavoriteEntity(locationId = location.locationId))
    }

    suspend fun removeFavorite(locationId: String) {
        try {
            apiService.removeFavorite(locationId)
        } catch (_: Exception) {
            // 离线时仅删本地
        }
        favoriteDao.delete(locationId)
    }

    suspend fun syncFromServer() {
        try {
            val remote = apiService.getFavorites()
            // 合并服务端收藏，不 deleteAll，避免覆盖用户本地收藏
            remote.forEach { loc ->
                favoriteDao.insert(FavoriteEntity(locationId = loc.locationId))
                locationDao.insertAll(listOf(loc.toEntity()))
            }
        } catch (_: Exception) {
            // 保留本地
        }
    }

    /** 收藏列表以 Room 中完整地点数据为准，避免展示过期字段 */
    suspend fun enrichFavoriteFromCache(locationId: String): Location? =
        locationDao.getLocationById(locationId)?.let { entity ->
            Location(
                locationId = entity.locationId,
                name = entity.name,
                latitude = entity.latitude,
                longitude = entity.longitude,
                category = entity.category,
                rating = entity.rating,
                openingHours = entity.openingHours,
                imageUrl = entity.imageUrl,
                hasGuide = entity.hasGuide
            )
        }

    private fun Location.toEntity() = com.example.guet_map.local.entity.LocationEntity(
        locationId = locationId,
        name = name,
        latitude = latitude,
        longitude = longitude,
        category = category,
        rating = rating,
        openingHours = openingHours,
        imageUrl = imageUrl,
        hasGuide = hasGuide
    )
}
