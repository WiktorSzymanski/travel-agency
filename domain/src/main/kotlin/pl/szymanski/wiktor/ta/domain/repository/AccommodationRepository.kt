package pl.szymanski.wiktor.ta.domain.repository

import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import java.util.UUID

interface AccommodationRepository {
    suspend fun findById(accommodationId: UUID): Accommodation?

    suspend fun save(accommodation: Accommodation): Accommodation
}
