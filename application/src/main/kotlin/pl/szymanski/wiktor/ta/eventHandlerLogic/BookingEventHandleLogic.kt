package pl.szymanski.wiktor.ta.eventHandlerLogic

import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.domain.event.BookingCancelRequestedEvent
import pl.szymanski.wiktor.ta.domain.event.BookingCreatedEvent
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import pl.szymanski.wiktor.ta.saga.SagaService
import pl.szymanski.wiktor.ta.saga.SagaState
import pl.szymanski.wiktor.ta.saga.SagaType


suspend fun onCreatedEvent(sagaService: SagaService, envelope: EventEnvelope<BookingCreatedEvent>) =
    sagaService.executeSaga(
        SagaState(
            correlationId = envelope.metadata.correlationId,
            type = SagaType.BOOKING,
            travelOffer = envelope.event.travelOffer,
            bookingId = BookingId.from(envelope.event.bookingId),
            seat = envelope.event.seat,
        )
    )

suspend fun onCancelRequestedEvent(sagaService: SagaService, envelope: EventEnvelope<BookingCancelRequestedEvent>) =
    sagaService.executeSaga(
        SagaState(
            correlationId = envelope.metadata.correlationId,
            type = SagaType.CANCELLING,
            travelOffer = envelope.event.travelOffer,
            bookingId = BookingId.from(envelope.event.bookingId),
            seat = envelope.event.seat,
        )
    )

