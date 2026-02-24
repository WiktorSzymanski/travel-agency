package pl.szymanski.wiktor.ta.eventHandlerLogic

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import kotlinx.coroutines.test.runTest
import pl.szymanski.wiktor.ta.CommandBus
import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.dlq.DeadLetterQueueRepository
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.aggregate.AccommodationId
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import pl.szymanski.wiktor.ta.domain.aggregate.CommuteId
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import pl.szymanski.wiktor.ta.domain.event.BookingCancelRequestedEvent
import pl.szymanski.wiktor.ta.domain.event.BookingCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.ProcessBookingEvent
import pl.szymanski.wiktor.ta.saga.BookingSaga
import pl.szymanski.wiktor.ta.saga.CancelBookingSaga
import pl.szymanski.wiktor.ta.saga.SagaRepository
import pl.szymanski.wiktor.ta.saga.SagaStatus
import pl.szymanski.wiktor.ta.saga.SagaStep
import pl.szymanski.wiktor.ta.saga.SagaType
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BookingEventHandleLogicTest {
    private val commandBus = mockk<CommandBus>()
    private val sagaRepository = mockk<SagaRepository>()
    private val sagaOutboxPort = mockk<SagaOutboxPort>()
    private val deadLetterQueueRepository = mockk<DeadLetterQueueRepository>()

    private fun createLogic() =
        BookingEventHandleLogic(
            commandBus = commandBus,
            sagaRepository = sagaRepository,
            sagaOutboxPort = sagaOutboxPort,
            deadLetterQueueRepository = deadLetterQueueRepository,
        )

    @Test
    fun `getSagaState should map booking created event to booking saga state`() {
        val bookingId = BookingId.generate()
        val travelOffer = TravelOffer(CommuteId.generate(), AccommodationId.generate())
        val seat = Seat.Any
        val correlationId = UUID.randomUUID()
        val metadata = Metadata(correlationId = correlationId, revision = 1)
        val event =
            BookingCreatedEvent(
                bookingId = bookingId,
                travelOffer = travelOffer,
                userId = UUID.randomUUID(),
                seat = seat,
            )

        val sagaState = createLogic().getSagaState(event, metadata)

        assertEquals(SagaType.BOOKING, sagaState.type)
        assertEquals(correlationId, sagaState.correlationId)
        assertEquals(travelOffer, sagaState.travelOffer)
        assertEquals(bookingId, sagaState.bookingId)
        assertEquals(seat, sagaState.seat)
        assertEquals(SagaStatus.NEW, sagaState.status)
        assertEquals(SagaStep.IDLE, sagaState.step)
    }

    @Test
    fun `getSagaState should map booking cancel requested event to cancel saga state`() {
        val bookingId = BookingId.generate()
        val travelOffer = TravelOffer(CommuteId.generate(), AccommodationId.generate())
        val seat = Seat.Any
        val correlationId = UUID.randomUUID()
        val metadata = Metadata(correlationId = correlationId, revision = 1)
        val event =
            BookingCancelRequestedEvent(
                bookingId = bookingId,
                travelOffer = travelOffer,
                seat = seat,
            )

        val sagaState = createLogic().getSagaState(event, metadata)

        assertEquals(SagaType.CANCELLING, sagaState.type)
        assertEquals(correlationId, sagaState.correlationId)
        assertEquals(travelOffer, sagaState.travelOffer)
        assertEquals(bookingId, sagaState.bookingId)
        assertEquals(seat, sagaState.seat)
    }

    @Test
    fun `getSagaState should throw for unsupported booking events`() {
        val bookingId = BookingId.generate()
        val metadata = Metadata(correlationId = UUID.randomUUID(), revision = 1)

        assertFailsWith<IllegalArgumentException> {
            createLogic().getSagaState(ProcessBookingEvent(bookingId = bookingId), metadata)
        }
    }

    @Test
    fun `onCreatedEvent should execute booking saga`() =
        runTest {
            mockkConstructor(BookingSaga::class)
            try {
                coEvery { anyConstructed<BookingSaga>().executeOrResume() } returns Unit

                val bookingId = BookingId.generate()
                val travelOffer = TravelOffer(CommuteId.generate(), AccommodationId.generate())
                val event =
                    BookingCreatedEvent(
                        bookingId = bookingId,
                        travelOffer = travelOffer,
                        userId = UUID.randomUUID(),
                        seat = Seat.Any,
                    )
                val metadata = Metadata(correlationId = UUID.randomUUID(), revision = 1)

                createLogic().onCreatedEvent(EventEnvelope(event, metadata))

                coVerify(exactly = 1) { anyConstructed<BookingSaga>().executeOrResume() }
            } finally {
                unmockkConstructor(BookingSaga::class)
            }
        }

    @Test
    fun `onCancelRequestedEvent should execute cancel booking saga`() =
        runTest {
            mockkConstructor(CancelBookingSaga::class)
            try {
                coEvery { anyConstructed<CancelBookingSaga>().executeOrResume() } returns Unit

                val bookingId = BookingId.generate()
                val travelOffer = TravelOffer(CommuteId.generate(), AccommodationId.generate())
                val event =
                    BookingCancelRequestedEvent(
                        bookingId = bookingId,
                        travelOffer = travelOffer,
                        seat = Seat.Any,
                    )
                val metadata = Metadata(correlationId = UUID.randomUUID(), revision = 1)

                createLogic().onCancelRequestedEvent(EventEnvelope(event, metadata))

                coVerify(exactly = 1) { anyConstructed<CancelBookingSaga>().executeOrResume() }
            } finally {
                unmockkConstructor(CancelBookingSaga::class)
            }
        }
}
