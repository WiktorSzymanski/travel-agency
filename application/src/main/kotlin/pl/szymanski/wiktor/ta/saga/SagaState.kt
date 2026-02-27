package pl.szymanski.wiktor.ta.saga

import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
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

enum class SagaStep {
    IDLE,
    PENDING_COMMUTE,
    PENDING_ACCOMMODATION,
    PENDING_ATTRACTION,
    COMPENSATING_ACCOMMODATION,
    COMPENSATING_COMMUTE,
}

data class SagaState(
    val id: UUID = UUID.randomUUID(),
    val correlationId: UUID = UUID.randomUUID(),
    val type: SagaType,
    var status: SagaStatus = SagaStatus.NEW,
    var step: SagaStep = SagaStep.IDLE,
    val travelOffer: TravelOffer,
    val bookingId: BookingId,
    val seat: Seat,
    var message: String? = null,
    var retryCount: Int = 0,
    var version: Long = 1
)
