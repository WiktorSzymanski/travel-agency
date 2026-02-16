package pl.szymanski.wiktor.ta.repository

import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
import pl.szymanski.wiktor.ta.domain.event.AttractionEvent
import java.util.UUID

interface AttractionRepository {
    suspend fun findById(id: AttractionId): Pair<Attraction, Long>

    suspend fun create(
        entity: Attraction,
        event: AttractionEvent,
    )

    suspend fun save(
        entity: Attraction,
        event: AttractionEvent,
        metadata: Metadata
    )
}
