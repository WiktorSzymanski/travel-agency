package pl.szymanski.wiktor.ta.event

import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.event.PublishableEvent
import java.util.UUID

sealed interface SagaEvent : PublishableEvent {
    val bookingId: UUID
    override val entityId: UUID get() = bookingId
}

data class BookingSagaStartedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val bookingId: UUID,
) : SagaEvent

data class BookingSagaFailedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val bookingId: UUID,
    val message: String,
) : SagaEvent

data class BookingSagaCompletedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val bookingId: UUID,
    val seat: Seat,
) : SagaEvent

data class BookingCancelSagaStartedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val bookingId: UUID,
) : SagaEvent

data class BookingCancelSagaFailedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val bookingId: UUID,
    val message: String,
) : SagaEvent

data class BookingCancelSagaCompletedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val bookingId: UUID,
    val seat: Seat,
) : SagaEvent
