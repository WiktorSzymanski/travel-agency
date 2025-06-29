package pl.szymanski.wiktor.ta.domain.repository

import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import java.util.UUID

interface AttractionRepository {
    suspend fun findById(attractionId: UUID): Attraction?

    suspend fun save(attraction: Attraction): Attraction
}
