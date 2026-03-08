package pl.szymanski.wiktor.ta.infrastructure.eventHandler

import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaHandler
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.domain.event.BookingCancelRequestedEvent
import pl.szymanski.wiktor.ta.domain.event.BookingCreatedEvent
import pl.szymanski.wiktor.ta.eventHandlerLogic.onCancelRequestedEvent
import pl.szymanski.wiktor.ta.eventHandlerLogic.onCreatedEvent
import pl.szymanski.wiktor.ta.saga.SagaService

@Service
@KafkaListener(topics = ["booking-events"], groupId = "travel-agency")
class BookingEventHandler(
    private val sagaService: SagaService,
) {
    companion object {
        private val log = LoggerFactory.getLogger(BookingEventHandler::class.java)
    }

    @KafkaHandler
    suspend fun onBookingEvent(envelope: EventEnvelope<*>) {
        when (envelope.event) {
            is BookingCreatedEvent -> onCreatedEvent(
                sagaService,
                @Suppress("UNCHECKED_CAST")
                envelope as EventEnvelope<BookingCreatedEvent>
            )

            is BookingCancelRequestedEvent -> onCancelRequestedEvent(
                sagaService,
                @Suppress("UNCHECKED_CAST")
                envelope as EventEnvelope<BookingCancelRequestedEvent>
            )

            else -> log.error("Received unhandled event type in booking-events topic: ${envelope.event::class.java}")
        }
    }
}
