package com.example.guet_map.module.social.domain.usecase

import com.example.guet_map.module.social.data.model.AnnouncementCategory
import com.example.guet_map.module.social.data.model.CampusAnnouncement
import com.example.guet_map.module.social.data.repository.CampusAnnouncementRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCampusAnnouncementsUseCase @Inject constructor(
    private val repository: CampusAnnouncementRepository
) {
    suspend fun seedIfNeeded() {
        repository.seedMockIfEmpty()
    }

    operator fun invoke(): Flow<List<CampusAnnouncement>> {
        return repository.getAllAnnouncements()
    }

    fun byCategory(category: AnnouncementCategory): Flow<List<CampusAnnouncement>> {
        return repository.getAnnouncementsByCategory(category)
    }
}

class MarkAnnouncementReadUseCase @Inject constructor(
    private val repository: CampusAnnouncementRepository
) {
    suspend operator fun invoke(id: String) {
        repository.markAsRead(id)
    }
}

class GetAnnouncementDetailUseCase @Inject constructor(
    private val repository: CampusAnnouncementRepository
) {
    suspend operator fun invoke(id: String): CampusAnnouncement? {
        return repository.getAnnouncementById(id)
    }
}
