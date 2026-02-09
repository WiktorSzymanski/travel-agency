package pl.szymanski.wiktor.ta.event

import kotlinx.serialization.Serializable
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import pl.szymanski.wiktor.ta.domain.event.PublishableEvent
import java.util.UUID

@Serializable
sealed interface SagaEvent : PublishableEvent

@Serializable
data class BookingSagaStartedEvent(
    @Serializable(with = pl.szymanski.wiktor.ta.domain.UUIDSerializer::class)
    override val eventId: UUID = UUID.randomUUID(),
    val bookingId: BookingId,
) : SagaEvent

@Serializable
data class BookingSagaFailedEvent(
    @Serializable(with = pl.szymanski.wiktor.ta.domain.UUIDSerializer::class)
    override val eventId: UUID = UUID.randomUUID(),
    val bookingId: BookingId,
    val message: String,
) : SagaEvent

@Serializable
data class BookingSagaCompletedEvent(
    @Serializable(with = pl.szymanski.wiktor.ta.domain.UUIDSerializer::class)
    override val eventId: UUID = UUID.randomUUID(),
    val bookingId: BookingId,
    val seat: Seat,
) : SagaEvent

@Serializable
data class BookingCancelSagaStartedEvent(
    @Serializable(with = pl.szymanski.wiktor.ta.domain.UUIDSerializer::class)
    override val eventId: UUID = UUID.randomUUID(),
    val bookingId: BookingId,
) : SagaEvent

@Serializable
data class BookingCancelSagaFailedEvent(
    @Serializable(with = pl.szymanski.wiktor.ta.domain.UUIDSerializer::class)
    override val eventId: UUID = UUID.randomUUID(),
    val bookingId: BookingId,
    val message: String,
) : SagaEvent

@Serializable
data class BookingCancelSagaCompletedEvent(
    @Serializable(with = pl.szymanski.wiktor.ta.domain.UUIDSerializer::class)
    override val eventId: UUID = UUID.randomUUID(),
    val bookingId: BookingId,
    val seat: Seat,
) : SagaEvent
