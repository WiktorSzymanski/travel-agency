package pl.szymanski.wiktor.ta.infrastructure.eventHandler

import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaHandler
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Service
import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.commands.booking.cancel.CancelBookingCommandHandler
import pl.szymanski.wiktor.ta.commands.booking.complete.CompleteBookingCommandHandler
import pl.szymanski.wiktor.ta.commands.booking.fail.FailBookingCommandHandler
import pl.szymanski.wiktor.ta.commands.booking.failCancel.FailCancelBookingCommandHandler
import pl.szymanski.wiktor.ta.event.*
import pl.szymanski.wiktor.ta.eventHandlerLogic.*

@Service
@KafkaListener(topics = ["saga-events"], groupId = "travel-agency")
class SagaEventHandler(
    private val completeBookingCommandHandler: CompleteBookingCommandHandler,
    private val failBookingCommandHandler: FailBookingCommandHandler,
    private val cancelBookingCommandHandler: CancelBookingCommandHandler,
    private val failCancelBookingCommandHandler: FailCancelBookingCommandHandler,
) {
    companion object {
        private val log = LoggerFactory.getLogger(SagaEventHandler::class.java)
    }

    @KafkaHandler
    suspend fun onSagaEvent(envelope: EventEnvelope<*>, acknowledgment: Acknowledgment) {
        when (envelope.event) {
            is BookingSagaCompletedEvent -> onBookingSagaCompletedEvent(
                completeBookingCommandHandler,
                envelope as EventEnvelope<BookingSagaCompletedEvent>
            )
            is BookingSagaFailedEvent -> onBookingSagaFailedEvent(
                failBookingCommandHandler,
                envelope as EventEnvelope<BookingSagaFailedEvent>
            )
            is BookingCancelSagaCompletedEvent -> onBookingCancelSagaCompletedEvent(
                cancelBookingCommandHandler,
                envelope as EventEnvelope<BookingCancelSagaCompletedEvent>
            )
            is BookingCancelSagaFailedEvent -> onBookingCancelSagaFailedEvent(
                failCancelBookingCommandHandler,
                envelope as EventEnvelope<BookingCancelSagaFailedEvent>
            )
            else -> log.error("Received unhandled event type in saga-events topic: ${envelope.event::class.java}")
        }

        acknowledgment.acknowledge()
    }
}
