package pl.szymanski.wiktor.ta.infrastructure.eventHandler

import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaHandler
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Service
import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.commands.accommodation.expire.ExpireAccommodationCommandHandler
import pl.szymanski.wiktor.ta.commands.attraction.expire.ExpireAttractionCommandHandler
import pl.szymanski.wiktor.ta.commands.commute.expire.ExpireCommuteCommandHandler
import pl.szymanski.wiktor.ta.event.AccommodationDateMetEvent
import pl.szymanski.wiktor.ta.event.AttractionDateMetEvent
import pl.szymanski.wiktor.ta.event.CommuteDateMetEvent
import pl.szymanski.wiktor.ta.eventHandlerLogic.onAccommodationDateMetEvent
import pl.szymanski.wiktor.ta.eventHandlerLogic.onAttractionDateMetEvent
import pl.szymanski.wiktor.ta.eventHandlerLogic.onCommuteDateMetEvent


@Service
@KafkaListener(topics = ["date-met-events"], groupId = "travel-agency")
class DateMetEventHandler(
    private val expireCommuteCommandHandler: ExpireCommuteCommandHandler,
    private val expireAccommodationCommandHandler: ExpireAccommodationCommandHandler,
    private val expireAttractionCommandHandler: ExpireAttractionCommandHandler,
) {
    companion object {
        private val log = LoggerFactory.getLogger(DateMetEventHandler::class.java)
    }

    @KafkaHandler
    suspend fun onDateMetEvent(envelope: EventEnvelope<*>, acknowledgment: Acknowledgment) {
        when (envelope.event) {
            is AccommodationDateMetEvent -> onAccommodationDateMetEvent(
                expireAccommodationCommandHandler,
                envelope as EventEnvelope<AccommodationDateMetEvent>
            )

            is CommuteDateMetEvent -> onCommuteDateMetEvent(
                expireCommuteCommandHandler,
                envelope as EventEnvelope<CommuteDateMetEvent>
            )

            is AttractionDateMetEvent -> onAttractionDateMetEvent(
                expireAttractionCommandHandler,
                envelope as EventEnvelope<AttractionDateMetEvent>
            )

            else -> log.warn("Received unhandled event type in date-met-events topic: ${envelope.event::class.java}")
        }

        acknowledgment.acknowledge()
    }
}
