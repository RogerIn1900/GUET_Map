package com.example.guet_map.repository

import com.example.guet_map.BuildConfig
import com.example.guet_map.local.dao.LocationDao
import com.example.guet_map.local.entity.LocationEntity
import com.example.guet_map.model.Location
import com.example.guet_map.model.Resource
import com.example.guet_map.network.ApiService
import com.example.guet_map.util.GuetCampusPoiLoader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepository @Inject constructor(
    private val apiService: ApiService,
    private val locationDao: LocationDao,
    private val campusPoiLoader: GuetCampusPoiLoader
) {

    fun getLocations(): Flow<Resource<List<Location>>> = flow {
        emit(Resource.Loading)
        val hasCached = locationDao.count() > 0
        try {
            val locations = loadRemoteLocations()
            locationDao.deleteAll()
            locationDao.insertAll(locations.map { it.toEntity() })
            emit(Resource.Success(locations))
        } catch (e: IOException) {
            if (hasCached) {
                emit(Resource.Success(locationDao.getAllLocations().first().map { it.toDomain() }))
            } else {
                emit(Resource.Error("网络不可用: ${e.localizedMessage}"))
            }
        } catch (e: Exception) {
            if (hasCached) {
                emit(Resource.Success(locationDao.getAllLocations().first().map { it.toDomain() }))
            } else {
                emit(Resource.Error("加载失败: ${e.localizedMessage}"))
            }
        }
    }

    fun getLocationsByCategory(category: String): Flow<Resource<List<Location>>> = flow {
        emit(Resource.Loading)
        try {
            val cachedAll = locationDao.getAllLocations().first()
            if (cachedAll.isNotEmpty()) {
                emit(Resource.Success(cachedAll.map { it.toDomain() }.filter { it.category == category }))
                return@flow
            }
            val remote = loadRemoteLocations()
            locationDao.insertAll(remote.map { it.toEntity() })
            emit(Resource.Success(remote.filter { it.category == category }))
        } catch (e: Exception) {
            val cached = locationDao.getLocationsByCategory(category).first()
            if (cached.isNotEmpty()) {
                emit(Resource.Success(cached.map { it.toDomain() }))
            } else {
                emit(Resource.Error("加载失败: ${e.localizedMessage}"))
            }
        }
    }

    fun observeCachedLocations(): Flow<List<Location>> =
        locationDao.getAllLocations().map { entities -> entities.map { it.toDomain() } }

    fun observeCachedLocationsByCategory(category: String): Flow<List<Location>> =
        locationDao.getLocationsByCategory(category).map { entities -> entities.map { it.toDomain() } }

    suspend fun getCachedLocationById(locationId: String): Location? =
        locationDao.getLocationById(locationId)?.toDomain()

    private suspend fun loadRemoteLocations(): List<Location> {
        val fromAmap = campusPoiLoader.loadGuetCampusLocations()
        if (fromAmap.isNotEmpty()) return fromAmap
        if (!BuildConfig.USE_MOCK_API) {
            return apiService.getLocations()
        }
        return emptyList()
    }

    private fun Location.toEntity() = LocationEntity(
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

    private fun LocationEntity.toDomain() = Location(
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
