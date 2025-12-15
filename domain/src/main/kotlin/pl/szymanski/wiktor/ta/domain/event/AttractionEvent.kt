package pl.szymanski.wiktor.ta.domain.event

import pl.szymanski.wiktor.ta.domain.LocationEnum
import java.time.LocalDateTime
import java.util.UUID

sealed interface AttractionEvent : DomainEvent {
    val attractionId: UUID
}

data class AttractionCreatedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val attractionId: UUID,
    val name: String,
    val location: LocationEnum,
    val date: LocalDateTime,
    val capacity: Int,
) : AttractionEvent

data class AttractionBookedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val attractionId: UUID,
    val bookingId: UUID,
) : AttractionEvent

data class AttractionBookingCanceledEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val attractionId: UUID,
    val bookingId: UUID,
) : AttractionEvent

data class AttractionExpiredEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val attractionId: UUID,
) : AttractionEvent

data class AttractionFullEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val attractionId: UUID,
) : AttractionEvent

data class AttractionAvailableEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val attractionId: UUID,
) : AttractionEvent

data class AttractionBookedCompensatedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val attractionId: UUID,
    val bookingId: UUID,
) : AttractionEvent

data class AttractionBookingCanceledCompensatedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val attractionId: UUID,
    val bookingId: UUID,
) : AttractionEvent
