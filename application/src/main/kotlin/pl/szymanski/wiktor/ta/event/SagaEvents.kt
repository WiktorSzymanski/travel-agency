package pl.szymanski.wiktor.ta.event

import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import pl.szymanski.wiktor.ta.domain.event.PublishableEvent
import java.util.UUID

interface SagaEvent : PublishableEvent

data class BookingSagaStartedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    val bookingId: BookingId,
) : SagaEvent

data class BookingSagaFailedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    val bookingId: BookingId,
    val message: String,
) : SagaEvent

data class BookingSagaCompletedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    val bookingId: BookingId,
    val seat: Seat,
) : SagaEvent

data class BookingCancelSagaStartedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    val bookingId: BookingId,
) : SagaEvent

data class BookingCancelSagaFailedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    val bookingId: BookingId,
    val message: String,
) : SagaEvent

data class BookingCancelSagaCompletedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    val bookingId: BookingId,
    val seat: Seat,
) : SagaEvent
