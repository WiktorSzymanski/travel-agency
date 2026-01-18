package pl.szymanski.wiktor.ta.eventHandlerLogic

import pl.szymanski.wiktor.ta.CommandBus
import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.domain.event.BookingCancelRequestedEvent
import pl.szymanski.wiktor.ta.domain.event.BookingCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.BookingEvent
import pl.szymanski.wiktor.ta.saga.BookingSaga
import pl.szymanski.wiktor.ta.saga.CancelBookingSaga
import pl.szymanski.wiktor.ta.saga.DeadLetterQueueRepository
import pl.szymanski.wiktor.ta.saga.SagaOutboxPort
import pl.szymanski.wiktor.ta.saga.SagaRepository
import pl.szymanski.wiktor.ta.saga.SagaState
import pl.szymanski.wiktor.ta.saga.SagaType

class BookingEventHandleLogic(
    private val commandBus: CommandBus,
    private val sagaRepository: SagaRepository,
    private val sagaOutboxPort: SagaOutboxPort,
    private val deadLetterQueueRepository: DeadLetterQueueRepository,
) {
    suspend fun onCreatedEvent(envelope: EventEnvelope<BookingCreatedEvent>) =
        BookingSaga(
            commandBus,
            sagaRepository,
            sagaOutboxPort,
            deadLetterQueueRepository,
            getSagaState(envelope.event),
            envelope.metadata
        ).executeOrResume()

    suspend fun onCancelRequestedEvent(envelope: EventEnvelope<BookingCancelRequestedEvent>) =
        CancelBookingSaga(
            commandBus,
            sagaRepository,
            sagaOutboxPort,
            deadLetterQueueRepository,
            getSagaState(envelope.event),
            envelope.metadata
        ).executeOrResume()

    fun getSagaState(event: BookingEvent) =
        when (event) {
            is BookingCreatedEvent -> SagaState(
                type = SagaType.BOOKING,
                travelOffer = event.travelOffer,
                bookingId = event.bookingId,
                seat = event.seat,
            )
            is BookingCancelRequestedEvent -> SagaState(
                type = SagaType.CANCELLING,
                travelOffer = event.travelOffer,
                bookingId = event.bookingId,
                seat = event.seat,
            )
            else -> throw IllegalArgumentException("Unsupported event type: ${event::class.simpleName} for getSagaState method")
        }
}
