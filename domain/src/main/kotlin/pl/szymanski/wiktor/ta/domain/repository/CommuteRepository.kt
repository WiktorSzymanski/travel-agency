package pl.szymanski.wiktor.ta.domain.repository

import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import java.util.UUID

interface CommuteRepository : Repository<Commute> {
    suspend fun findById(commuteId: UUID): Commute?

    override suspend fun save(entity: Commute): Commute?

    suspend fun findAll(): List<Commute>

    suspend fun updateAllStatus(commutes: List<Commute>)
}
