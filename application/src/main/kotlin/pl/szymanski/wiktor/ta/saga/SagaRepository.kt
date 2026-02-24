package pl.szymanski.wiktor.ta.saga

import pl.szymanski.wiktor.ta.repository.Repository
import java.util.UUID

interface SagaRepository : Repository<SagaState> {
    suspend fun findById(id: UUID): SagaState?

    suspend fun findByStatuses(statuses: List<SagaStatus>): List<SagaState>

    suspend fun save(saga: SagaState)
}
