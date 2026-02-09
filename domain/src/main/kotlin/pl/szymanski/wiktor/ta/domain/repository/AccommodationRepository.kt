package pl.szymanski.wiktor.ta.domain.repository

import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.AccommodationId
import pl.szymanski.wiktor.ta.domain.event.AccommodationEvent

interface AccommodationRepository {
    suspend fun findById(id: AccommodationId): Accommodation

    suspend fun create(
        entity: Accommodation,
        event: AccommodationEvent,
    )

    suspend fun save(
        entity: Accommodation,
        event: AccommodationEvent,
    )
}
