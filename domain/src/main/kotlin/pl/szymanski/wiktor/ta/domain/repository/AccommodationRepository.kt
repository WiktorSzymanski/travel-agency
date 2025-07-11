package pl.szymanski.wiktor.ta.domain.repository

import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import java.util.UUID

interface AccommodationRepository : Repository<Accommodation> {
    suspend fun findById(accommodationId: UUID): Accommodation?

    override suspend fun save(entity: Accommodation): Accommodation?

    suspend fun findAll(): List<Accommodation>

    suspend fun updateAllStatus(accommodations: List<Accommodation>)
}
