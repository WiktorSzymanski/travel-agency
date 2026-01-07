package pl.szymanski.wiktor.ta.saga

import java.util.UUID

enum class SagaType {
    BOOKING,
    CANCELLING
}

enum class SagaStatus {
    NEW,
    PROCESSING,
    COMPLETED,
    FAILED,
}

data class SagaState(
    private val id: UUID,
    private val type: SagaType,
    private val status: SagaStatus,
    private val data: String,
    private val version: Int
)