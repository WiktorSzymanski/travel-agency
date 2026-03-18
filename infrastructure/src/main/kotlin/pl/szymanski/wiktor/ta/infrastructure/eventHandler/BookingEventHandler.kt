package pl.szymanski.wiktor.ta.infrastructure.eventHandler

import kotlinx.coroutines.runBlocking
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
    fun onBookingEvent(envelope: EventEnvelope<*>) = runBlocking {
        when (envelope.eventType) {
            "BookingCreatedEvent" -> onCreatedEvent(
                sagaService,
                @Suppress("UNCHECKED_CAST")
                envelope as EventEnvelope<BookingCreatedEvent>
            )

            "BookingCancelRequestedEvent" -> onCancelRequestedEvent(
                sagaService,
                @Suppress("UNCHECKED_CAST")
                envelope as EventEnvelope<BookingCancelRequestedEvent>
            )

            else -> log.warn("Received unhandled event type in booking-events topic: ${envelope.event::class.java}")
        }
    }
}
