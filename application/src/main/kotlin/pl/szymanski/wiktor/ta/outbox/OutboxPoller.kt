package pl.szymanski.wiktor.ta.outbox

import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.dlq.DeadLetterQueueEntry
import pl.szymanski.wiktor.ta.dlq.DeadLetterQueueRepository
import pl.szymanski.wiktor.ta.domain.event.PublishableEvent
import java.util.UUID

class OutboxPoller(
    private val sagaOutboxPort: SagaOutboxPort,
    private val eventBus: EventBus,
    private val deadLetterQueueRepository: DeadLetterQueueRepository
) {
    companion object {
        private const val BATCH_SIZE = 100
        private const val MAX_PUBLISH_RETRIES = 3
    }

    private val log = LoggerFactory.getLogger(this.javaClass)

    suspend fun pollAndPublishAll() {
        do {
            val count = pollAndPublish()
        } while (count > 0)
    }

    suspend fun pollAndPublish(): Long {
        log.debug("Starting outbox poll batch (limit=$BATCH_SIZE)")

        val pendingOutboxEntries = sagaOutboxPort
            .getPendingEvents(BATCH_SIZE)

        if (pendingOutboxEntries.isEmpty()) {
            log.trace("No pending outbox events")
            return 0L
        }

        log.info("Processing ${pendingOutboxEntries.size} pending events")

        pendingOutboxEntries.forEach { outboxEntry ->
            publishEventWithRetry(outboxEntry.eventEnvelope)?.let { publishedEventId ->
                /**
                    Jeśli się wywali przed oznaczeniem jako published, znowu by próbował publishować event
                    więc zakładamy że EventBus/EventStore deduplikuje eventy, co za tym idzie operacja publish
                    powinna kończyć się pozytywnie gdy już event o tym ID istnieje/to trzeba gdzieś obsłuźyć
                **/
                sagaOutboxPort.markAsPublished(publishedEventId)
            }
        }

        return pendingOutboxEntries.size.toLong()
    }

    private suspend fun publishEventWithRetry(
        eventEnvelope: EventEnvelope<PublishableEvent>,
        retryCount: Int = 0
    ): UUID? {
        try {
            eventBus.publish(eventEnvelope)
            log.debug("Published event {} ({})", eventEnvelope.event.eventId, eventEnvelope.eventType)

            return eventEnvelope.event.eventId
        } catch (e: Exception) {
            log.warn("Failed to publish ${eventEnvelope.event.eventId} (attempt ${retryCount + 1}): ${e.message}")

            if (retryCount < MAX_PUBLISH_RETRIES) {
                delay((retryCount + 1) * 1000L)
                return publishEventWithRetry(eventEnvelope, retryCount + 1)
            }

            log.error("Max retries exceeded for ${eventEnvelope.event.eventId}}")
            deadLetterQueueRepository.save(DeadLetterQueueEntry(eventEnvelope.toString()))

            return null
        }
    }
}
