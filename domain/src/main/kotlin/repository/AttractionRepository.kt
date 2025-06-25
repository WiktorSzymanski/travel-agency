package repository

import aggregate.Attraction
import java.util.UUID

interface AttractionRepository {
    fun findById(attractionId: UUID): Attraction?

    fun save(attraction: Attraction)
}
