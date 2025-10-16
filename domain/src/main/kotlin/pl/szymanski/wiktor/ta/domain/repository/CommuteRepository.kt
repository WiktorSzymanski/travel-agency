package pl.szymanski.wiktor.ta.domain.repository

import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.event.CommuteEvent
import java.util.UUID

interface CommuteRepository {
    suspend fun findById(id: UUID): Commute

    suspend fun create(
        entity: Commute,
        event: CommuteEvent,
    )

    suspend fun save(
        entity: Commute,
        event: CommuteEvent,
    )
}
