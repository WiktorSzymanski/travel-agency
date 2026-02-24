package pl.szymanski.wiktor.ta.repository

import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.aggregate.CommuteId
import pl.szymanski.wiktor.ta.domain.event.CommuteEvent

interface CommuteRepository : Repository<Commute> {
    suspend fun findById(id: CommuteId): Pair<Commute, Long>
}
