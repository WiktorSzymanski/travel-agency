package pl.szymanski.wiktor.ta.domain.repository

import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
import pl.szymanski.wiktor.ta.domain.event.AttractionEvent
import java.util.UUID

interface AttractionRepository {
    suspend fun findById(id: AttractionId): Attraction

    suspend fun create(
        entity: Attraction,
        event: AttractionEvent,
    )

    suspend fun save(
        entity: Attraction,
        event: AttractionEvent,
    )
}
