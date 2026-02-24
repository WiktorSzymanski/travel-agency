package pl.szymanski.wiktor.ta.repository

import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.AccommodationId
import pl.szymanski.wiktor.ta.domain.event.AccommodationEvent

interface AccommodationRepository : Repository<Accommodation> {
    suspend fun findById(id: AccommodationId): Pair<Accommodation, Long>
}
