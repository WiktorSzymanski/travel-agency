package pl.szymanski.wiktor.ta.repository

import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.AccommodationId
import pl.szymanski.wiktor.ta.domain.event.AccommodationEvent

interface AccommodationRepository {
    suspend fun findById(id: AccommodationId): Pair<Accommodation, Long>

    suspend fun create(
        entity: Accommodation,
        event: AccommodationEvent,
    )

    suspend fun save(
        entity: Accommodation,
        event: AccommodationEvent,
        metadata: Metadata
    )
}
