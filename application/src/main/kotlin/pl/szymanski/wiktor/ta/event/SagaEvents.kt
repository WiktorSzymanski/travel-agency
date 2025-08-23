package pl.szymanski.wiktor.ta.event

import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.event.Event
import java.util.UUID

data class BookingSagaStartedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID?,
    val bookingId: UUID,
) : Event

data class BookingSagaFailedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID?,
    val bookingId: UUID,
    val message: String,
) : Event

data class BookingSagaCompletedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID?,
    val bookingId: UUID,
    val travelOfferId: UUID,
    val seat: Seat?
) : Event

data class CancelBookingSagaStartedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID?,
    val bookingId: UUID,
) : Event

data class CancelBookingSagaFailedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID?,
    val bookingId: UUID,
    val message: String,
) : Event

data class CancelBookingSagaCompletedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID?,
    val bookingId: UUID,
    val travelOfferId: UUID,
    val seat: Seat?
) : Event

