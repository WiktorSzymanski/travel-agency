package pl.szymanski.wiktor.ta.domain.event

import pl.szymanski.wiktor.ta.domain.LocationEnum
import java.time.LocalDateTime
import java.util.UUID

data class AttractionBookedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val attractionId: UUID,
    val userId: UUID,
) : AttractionEvent

data class AttractionBookingCanceledEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val attractionId: UUID,
    val userId: UUID,
) : AttractionEvent

data class AttractionExpiredEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val attractionId: UUID,
) : AttractionEvent

data class AttractionCreatedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val attractionId: UUID,
    val name: String,
    val location: LocationEnum,
    val date: LocalDateTime,
    val capacity: Int,
) : AttractionEvent
