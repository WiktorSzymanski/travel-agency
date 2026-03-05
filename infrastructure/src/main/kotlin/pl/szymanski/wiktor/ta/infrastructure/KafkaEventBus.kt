package pl.szymanski.wiktor.ta.infrastructure

import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.domain.event.AccommodationEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionEvent
import pl.szymanski.wiktor.ta.domain.event.BookingEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteEvent
import pl.szymanski.wiktor.ta.domain.event.PublishableEvent
import pl.szymanski.wiktor.ta.event.DateMetEvent
import pl.szymanski.wiktor.ta.event.SagaEvent
import java.time.LocalDateTime

@Component
class KafkaEventBus(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) : EventBus {

    override suspend fun publish(event: EventEnvelope<out PublishableEvent>) {
        kafkaTemplate.send(topicResolver(event.event), event.event.entityId.toString(), event)
    }

    suspend fun publish2(event: EventEnvelope2<out PublishableEvent>) {
        kafkaTemplate.send(topicResolver2(event.event), event.event.entityId.toString(), event)
    }

    override suspend fun publishAtGivenTime(event: EventEnvelope<out PublishableEvent>, date: LocalDateTime) {
        throw UnsupportedOperationException("Publishing events at a given time is not supported in this implementation")
    }

    private fun topicResolver(event: PublishableEvent): String = when (event) {
        is AccommodationEvent -> "accommodation-events"
        is AttractionEvent -> "attraction-events"
        is CommuteEvent -> "commute-events"
        is BookingEvent -> "booking-events"
        is SagaEvent -> "saga-events"
        is DateMetEvent -> "date-met-events"
        else -> throw IllegalArgumentException("No topic defined for event type: ${event::class.simpleName}")
    }

    private fun topicResolver2(event: PublishableEvent): String = when (event) {
        is DateMetEvent2 -> "date-met-events-2"
        else -> throw IllegalArgumentException("No topic defined for event type: ${event::class.simpleName}")
    }
}
