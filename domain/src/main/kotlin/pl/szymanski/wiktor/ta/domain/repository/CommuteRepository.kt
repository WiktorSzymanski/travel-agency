package pl.szymanski.wiktor.ta.domain.repository

import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import java.util.UUID

interface CommuteRepository {
    fun findById(commuteId: UUID): Commute?

    fun save(commute: Commute)
}
