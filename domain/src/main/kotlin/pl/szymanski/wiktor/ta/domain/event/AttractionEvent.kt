package pl.szymanski.wiktor.ta.domain.event

import pl.szymanski.wiktor.ta.domain.LocationEnum
import java.time.LocalDateTime
import java.util.UUID

interface AttractionFailedEvent : AttractionEvent, FailedEvent

data class AttractionCreatedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val attractionId: UUID,
    val name: String,
    val location: LocationEnum,
    val date: LocalDateTime,
    val capacity: Int,
) : AttractionEvent

data class AttractionBookedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val attractionId: UUID,
    val bookingId: UUID,
) : AttractionEvent

data class AttractionBookingCanceledEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val attractionId: UUID,
    val bookingId: UUID,
) : AttractionEvent

data class AttractionExpiredEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val attractionId: UUID,
) : AttractionEvent


data class AttractionFullEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val attractionId: UUID,
) : AttractionEvent

data class AttractionAvailableEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val attractionId: UUID,
) : AttractionEvent

data class AttractionBookFailedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val attractionId: UUID,
    val bookingId: UUID,
    override val message: String,
) : AttractionFailedEvent

data class AttractionBookingCancelFailedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val attractionId: UUID,
    val bookingId: UUID,
    override val message: String,
) : AttractionFailedEvent

data class AttractionExpireFailedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val attractionId: UUID,
    override val message: String,
) : AttractionFailedEvent