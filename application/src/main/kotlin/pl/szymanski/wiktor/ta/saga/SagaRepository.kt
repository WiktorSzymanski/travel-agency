package pl.szymanski.wiktor.ta.saga

import java.util.UUID

interface SagaRepository {
    suspend fun findById(id: UUID): SagaState?

    suspend fun findByStatus(status: SagaStatus): List<SagaState>

    suspend fun save(saga: SagaState)
}