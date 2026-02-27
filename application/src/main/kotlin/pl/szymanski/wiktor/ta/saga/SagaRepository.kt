package pl.szymanski.wiktor.ta.saga

import pl.szymanski.wiktor.ta.repository.CommandRepository
import java.util.UUID

interface SagaRepository : CommandRepository<SagaState, UUID> {
    suspend fun findByStatuses(statuses: List<SagaStatus>): List<SagaState>
    suspend fun save(saga: SagaState)
}
