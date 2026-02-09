package pl.szymanski.wiktor.ta.domain.event

import kotlinx.serialization.Serializable
import pl.szymanski.wiktor.ta.domain.UUIDSerializer
import pl.szymanski.wiktor.ta.domain.BookingState
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import java.util.UUID

@Serializable
sealed interface BookingEvent : DomainEvent {
    val bookingId: BookingId
}

@Serializable
data class BookingCreatedEvent(
    @Serializable(with = UUIDSerializer::class)
    override val eventId: UUID = UUID.randomUUID(),
    override val bookingId: BookingId,
    val travelOffer: TravelOffer,
    @Serializable(with = UUIDSerializer::class)
    val userId: UUID,
    val seat: Seat,
    val state: BookingState = BookingState.NEW
) : BookingEvent

@Serializable
data class ProcessBookingEvent(
    @Serializable(with = UUIDSerializer::class)
    override val eventId: UUID = UUID.randomUUID(),
    override val bookingId: BookingId,
) : BookingEvent

@Serializable
data class CompleteBookingEvent(
    @Serializable(with = UUIDSerializer::class)
    override val eventId: UUID = UUID.randomUUID(),
    override val bookingId: BookingId,
) : BookingEvent

@Serializable
data class BookingCancelRequestedEvent(
    @Serializable(with = UUIDSerializer::class)
    override val eventId: UUID = UUID.randomUUID(),
    override val bookingId: BookingId,
    val travelOffer: TravelOffer,
    val seat: Seat,
) : BookingEvent

@Serializable
data class CancelBookingEvent(
    @Serializable(with = UUIDSerializer::class)
    override val eventId: UUID = UUID.randomUUID(),
    override val bookingId: BookingId,
) : BookingEvent

@Serializable
data class ProcessCancelBookingEvent(
    @Serializable(with = UUIDSerializer::class)
    override val eventId: UUID = UUID.randomUUID(),
    override val bookingId: BookingId,
) : BookingEvent

@Serializable
data class FailBookingEvent(
    @Serializable(with = UUIDSerializer::class)
    override val eventId: UUID = UUID.randomUUID(),
    override val bookingId: BookingId,
    val message: String,
) : BookingEvent

@Serializable
data class FailCancelBookingEvent(
    @Serializable(with = UUIDSerializer::class)
    override val eventId: UUID = UUID.randomUUID(),
    override val bookingId: BookingId,
    val message: String,
) : BookingEvent
