package pl.szymanski.wiktor.ta.domain.repository

import pl.szymanski.wiktor.ta.domain.AttractionStatusEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.event.Event
import java.util.UUID

interface AttractionRepository : Repository<Attraction> {
    suspend fun findById(attractionId: UUID): Attraction
}
