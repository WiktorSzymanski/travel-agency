package pl.szymanski.wiktor.ta.domain.repository

import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import java.util.UUID

interface AttractionRepository : Repository<Attraction> {
    suspend fun findById(attractionId: UUID): Attraction

    override suspend fun save(entity: Attraction): Attraction?

    suspend fun update(entity: Attraction)

    suspend fun findAll(): List<Attraction>

    suspend fun updateAllStatus(attractions: List<Attraction>)
}
