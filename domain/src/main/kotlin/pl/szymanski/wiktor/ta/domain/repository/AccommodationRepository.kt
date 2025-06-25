package pl.szymanski.wiktor.ta.domain.repository

import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import java.util.UUID

interface AccommodationRepository {
    fun findById(accommodationId: UUID): Accommodation?

    fun save(accommodation: Accommodation)
}
