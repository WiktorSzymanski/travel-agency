package pl.szymanski.wiktor.ta.repository

import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
import pl.szymanski.wiktor.ta.domain.event.AttractionEvent

interface AttractionRepository : Repository<Attraction> {
    suspend fun findById(id: AttractionId): Pair<Attraction, Long>
}
