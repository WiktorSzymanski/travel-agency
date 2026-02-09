package pl.szymanski.wiktor.ta.domain.event

import kotlinx.serialization.Serializable
import pl.szymanski.wiktor.ta.domain.LocalDateTimeSerializer
import pl.szymanski.wiktor.ta.domain.UUIDSerializer
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import java.time.LocalDateTime
import java.util.UUID

@Serializable
sealed interface AttractionEvent : DomainEvent {
    val attractionId: AttractionId
}

@Serializable
data class AttractionCreatedEvent(
    @Serializable(with = UUIDSerializer::class)
    override val eventId: UUID = UUID.randomUUID(),
    override val attractionId: AttractionId,
    val name: String,
    val location: LocationEnum,
    @Serializable(with = LocalDateTimeSerializer::class)
    val date: LocalDateTime,
    val capacity: Int,
) : AttractionEvent

@Serializable
data class AttractionBookedEvent(
    @Serializable(with = UUIDSerializer::class)
    override val eventId: UUID = UUID.randomUUID(),
    override val attractionId: AttractionId,
    val bookingId: BookingId,
) : AttractionEvent

@Serializable
data class AttractionBookingCanceledEvent(
    @Serializable(with = UUIDSerializer::class)
    override val eventId: UUID = UUID.randomUUID(),
    override val attractionId: AttractionId,
    val bookingId: BookingId,
) : AttractionEvent

@Serializable
data class AttractionExpiredEvent(
    @Serializable(with = UUIDSerializer::class)
    override val eventId: UUID = UUID.randomUUID(),
    override val attractionId: AttractionId,
) : AttractionEvent

@Serializable
data class AttractionFullEvent(
    @Serializable(with = UUIDSerializer::class)
    override val eventId: UUID = UUID.randomUUID(),
    override val attractionId: AttractionId,
) : AttractionEvent

@Serializable
data class AttractionAvailableEvent(
    @Serializable(with = UUIDSerializer::class)
    override val eventId: UUID = UUID.randomUUID(),
    override val attractionId: AttractionId,
) : AttractionEvent

@Serializable
data class AttractionBookedCompensatedEvent(
    @Serializable(with = UUIDSerializer::class)
    override val eventId: UUID = UUID.randomUUID(),
    override val attractionId: AttractionId,
    val bookingId: BookingId,
) : AttractionEvent

@Serializable
data class AttractionBookingCanceledCompensatedEvent(
    @Serializable(with = UUIDSerializer::class)
    override val eventId: UUID = UUID.randomUUID(),
    override val attractionId: AttractionId,
    val bookingId: BookingId,
) : AttractionEvent
