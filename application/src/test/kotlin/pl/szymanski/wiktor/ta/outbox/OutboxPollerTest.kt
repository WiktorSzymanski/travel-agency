package pl.szymanski.wiktor.ta.outbox

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.dlq.DeadLetterQueueRepository
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import pl.szymanski.wiktor.ta.domain.event.ProcessBookingEvent
import pl.szymanski.wiktor.ta.domain.event.PublishableEvent
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class OutboxPollerTest {
    private val sagaOutboxPort = mockk<SagaOutboxPort>()
    private val eventBus = mockk<EventBus>()
    private val deadLetterQueueRepository = mockk<DeadLetterQueueRepository>()
    private lateinit var poller: OutboxPoller

    @BeforeTest
    fun setup() {
        poller = OutboxPoller(sagaOutboxPort, eventBus, deadLetterQueueRepository)
    }

    @Test
    fun `pollAndPublish returns zero when no pending events`() = runTest {
        coEvery { sagaOutboxPort.getPendingEvents(any()) } returns emptyList()

        val count = poller.pollAndPublish()

        assertEquals(0, count)
        coVerify(exactly = 0) { eventBus.publish(any()) }
        coVerify(exactly = 0) { sagaOutboxPort.markAsPublished(any()) }
        coVerify(exactly = 0) { deadLetterQueueRepository.save(any()) }
    }

    @Test
    fun `pollAndPublish publishes and marks all pending events`() = runTest {
        val entry1 = outboxEntry()
        val entry2 = outboxEntry()

        coEvery { sagaOutboxPort.getPendingEvents(any()) } returns listOf(entry1, entry2)
        coEvery { eventBus.publish(any()) } just Runs
        coEvery { sagaOutboxPort.markAsPublished(any()) } just Runs

        val count = poller.pollAndPublish()

        assertEquals(2, count)
        coVerify(exactly = 1) { eventBus.publish(entry1.eventEnvelope) }
        coVerify(exactly = 1) { eventBus.publish(entry2.eventEnvelope) }
        coVerify(exactly = 1) { sagaOutboxPort.markAsPublished(entry1.eventId) }
        coVerify(exactly = 1) { sagaOutboxPort.markAsPublished(entry2.eventId) }
        coVerify(exactly = 0) { deadLetterQueueRepository.save(any()) }
    }

    @Test
    fun `pollAndPublish retries and succeeds without DLQ`() = runTest {
        val entry = outboxEntry()

        coEvery { sagaOutboxPort.getPendingEvents(any()) } returns listOf(entry)
        coEvery { sagaOutboxPort.markAsPublished(any()) } just Runs
        coEvery { deadLetterQueueRepository.save(any()) } just Runs
        coEvery { eventBus.publish(any()) } throws RuntimeException("fail") andThenThrows RuntimeException("fail") andThen Unit

        val count = poller.pollAndPublish()

        assertEquals(1, count)
        coVerify(exactly = 3) { eventBus.publish(entry.eventEnvelope) }
        coVerify(exactly = 1) { sagaOutboxPort.markAsPublished(entry.eventId) }
        coVerify(exactly = 0) { deadLetterQueueRepository.save(any()) }
    }

    @Test
    fun `pollAndPublish sends to DLQ after max retries`() = runTest {
        val entry = outboxEntry()

        coEvery { sagaOutboxPort.getPendingEvents(any()) } returns listOf(entry)
        coEvery { deadLetterQueueRepository.save(any()) } just Runs
        coEvery { eventBus.publish(any()) } throws RuntimeException("fail")

        val count = poller.pollAndPublish()

        assertEquals(1, count)
        coVerify(exactly = 4) { eventBus.publish(entry.eventEnvelope) }
        coVerify(exactly = 0) { sagaOutboxPort.markAsPublished(any()) }
        coVerify(exactly = 1) {
            deadLetterQueueRepository.save(match { it.message == entry.eventEnvelope.toString() })
        }
    }

    private fun outboxEntry(
        event: PublishableEvent = ProcessBookingEvent(bookingId = BookingId.generate())
    ): SagaOutboxEntry {
        val envelope = EventEnvelope(event, Metadata(UUID.randomUUID(), 0))
        return SagaOutboxEntry(
            eventId = event.eventId,
            sagaId = UUID.randomUUID(),
            eventEnvelope = envelope,
            published = false,
            publishedAt = null,
        )
    }
}
