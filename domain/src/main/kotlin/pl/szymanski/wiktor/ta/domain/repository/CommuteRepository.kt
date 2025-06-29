package pl.szymanski.wiktor.ta.domain.repository

import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import java.util.UUID

interface CommuteRepository {
    suspend fun findById(commuteId: UUID): Commute?

    suspend fun save(commute: Commute): Commute
}
