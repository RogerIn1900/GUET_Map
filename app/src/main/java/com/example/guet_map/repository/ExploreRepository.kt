package com.example.guet_map.repository

import com.example.guet_map.model.Location
import com.example.guet_map.model.RecentGuide
import com.example.guet_map.model.Resource
import com.example.guet_map.network.ApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExploreRepository @Inject constructor(
    private val apiService: ApiService,
    private val locationRepository: LocationRepository
) {

    fun getHotLocations(): Flow<List<Location>> =
        locationRepository.observeCachedLocations().map { locations ->
            locations
                .sortedWith(
                    compareByDescending<Location> { it.hasGuide }
                        .thenByDescending { it.rating }
                )
                .take(10)
        }

    fun getRecentGuides(): Flow<Resource<List<RecentGuide>>> = flow {
        emit(Resource.Loading)
        try {
            emit(Resource.Success(apiService.getRecentGuides()))
        } catch (e: Exception) {
            emit(Resource.Error("加载失败: ${e.localizedMessage}"))
        }
    }
}
