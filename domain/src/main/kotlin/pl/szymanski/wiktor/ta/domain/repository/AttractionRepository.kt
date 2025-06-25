package pl.szymanski.wiktor.ta.domain.repository

import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import java.util.UUID

interface AttractionRepository {
    fun findById(attractionId: UUID): Attraction?

    fun save(attraction: Attraction)
}
