package pl.szymanski.wiktor.ta.domain.repository

import pl.szymanski.wiktor.ta.domain.CommuteStatusEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.event.Event
import java.util.UUID

interface CommuteRepository : Repository<Commute> {
    suspend fun findById(commuteId: UUID): Commute

    override suspend fun save(event: Event)
}
