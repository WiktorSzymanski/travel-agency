package repository

import aggregate.Accommodation
import java.util.UUID

interface AccommodationRepository {
    fun findById(accommodationId: UUID): Accommodation?
    fun save(accommodation: Accommodation)
}