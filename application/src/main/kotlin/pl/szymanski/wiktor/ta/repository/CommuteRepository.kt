package pl.szymanski.wiktor.ta.repository

import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.aggregate.CommuteId
import pl.szymanski.wiktor.ta.domain.event.CommuteEvent
import java.util.UUID

interface CommuteRepository {
    suspend fun findById(id: CommuteId): Pair<Commute, Long>

    suspend fun create(
        entity: Commute,
        event: CommuteEvent
    )

    suspend fun save(
        entity: Commute,
        event: CommuteEvent,
        metadata: Metadata
    )
}
