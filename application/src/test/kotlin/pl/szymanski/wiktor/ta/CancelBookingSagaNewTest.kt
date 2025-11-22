package pl.szymanski.wiktor.ta

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.Event
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReleaseEvent
import pl.szymanski.wiktor.ta.domain.repository.EventRepository
import pl.szymanski.wiktor.ta.event.BookingCancelSagaCompletedEvent
import pl.szymanski.wiktor.ta.event.BookingCancelSagaFailedEvent
import pl.szymanski.wiktor.ta.event.BookingCancelSagaStartedEvent
import pl.szymanski.wiktor.ta.saga.CancelBookingSaga
import pl.szymanski.wiktor.ta.service.TravelOfferService
import pl.szymanski.wiktor.ta.commandHandler.AccommodationCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.AttractionCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.CommuteCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.TravelOfferCommandHandler
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CancelBookingSagaNewTest {

    private class InMemoryEventRepository : EventRepository {
        val events = mutableListOf<Event>()

        override suspend fun save(event: Event, revision: Int) {
            events.add(event)
        }

        override suspend fun noRevisionSave(event: Event) {
            events.add(event)
        }

        override suspend fun subscribe(
            eventClass: Class<Event>,
            positionPair: Pair<Long, Long>,
            doOnEvent: suspend (Event) -> Unit,
        ) {
            // not used in tests
        }
    }

    private lateinit var repo: InMemoryEventRepository

    @BeforeTest
    fun setup() {
        repo = InMemoryEventRepository()
        EventBus.init(repo)
    }

    private fun releaseEvent(
        travelOfferId: UUID = UUID.randomUUID(),
        accommodationId: UUID = UUID.randomUUID(),
        commuteId: UUID = UUID.randomUUID(),
        attractionId: UUID? = UUID.randomUUID(),
        bookingId: UUID = UUID.randomUUID(),
        correlationId: UUID = UUID.randomUUID(),
        seat: Seat = Seat("1", "A"),
    ) = TravelOfferReleaseEvent(
        travelOfferId = travelOfferId,
        accommodationId = accommodationId,
        commuteId = commuteId,
        attractionId = attractionId,
        bookingId = bookingId,
        seat = seat,
        correlationId = correlationId,
    )

    @Test
    fun `cancel saga success with attraction should complete and publish events`() = runTest {
        // Given
        val triggering = releaseEvent(attractionId = UUID.randomUUID())

        val travelOfferCommandHandler = mockk<TravelOfferCommandHandler>(relaxed = true)
        val attractionHandler = mockk<AttractionCommandHandler>(relaxed = true)
        val commuteHandler = mockk<CommuteCommandHandler>(relaxed = true)
        val accommodationHandler = mockk<AccommodationCommandHandler>(relaxed = true)
        val travelOfferService = mockk<TravelOfferService>(relaxed = true)

        val commuteAgg = mockk<Commute>(relaxed = true)
        val accommodationAgg = mockk<Accommodation>(relaxed = true)
        val attractionAgg = mockk<Attraction>(relaxed = true)

        coEvery { commuteHandler.handle(any()) } returns (commuteAgg to listOf(
            CommuteBookingCanceledEvent(
                commuteId = triggering.commuteId,
                bookingId = triggering.bookingId,
                seat = triggering.seat!!,
            )
        ))

        coEvery { accommodationHandler.handle(any()) } returns (accommodationAgg to listOf(
            AccommodationBookingCanceledEvent(
                accommodationId = triggering.accommodationId,
                bookingId = triggering.bookingId,
            )
        ))

        coEvery { attractionHandler.handle(any()) } returns (attractionAgg to listOf(
            AttractionBookingCanceledEvent(
                attractionId = triggering.attractionId!!,
                bookingId = triggering.bookingId,
            )
        ))

        val saga = CancelBookingSaga(
            travelOfferCommandHandler,
            attractionHandler,
            commuteHandler,
            accommodationHandler,
            travelOfferService,
            triggering,
        )

        // When
        saga.execute()

        // Then
        val started = repo.events.filterIsInstance<BookingCancelSagaStartedEvent>()
        val completed = repo.events.filterIsInstance<BookingCancelSagaCompletedEvent>()
        val failed = repo.events.filterIsInstance<BookingCancelSagaFailedEvent>()

        assertEquals(1, started.size)
        assertEquals(1, completed.size)
        assertTrue(failed.isEmpty())
        assertEquals(triggering.bookingId, completed.first().bookingId)
        assertEquals(triggering.travelOfferId, completed.first().travelOfferId)

        coVerify(exactly = 1) { commuteHandler.handle(any()) }
        coVerify(exactly = 1) { accommodationHandler.handle(any()) }
        coVerify(exactly = 1) { attractionHandler.handle(any()) }
    }

    @Test
    fun `cancel saga success without attraction should complete and not call attraction handler`() = runTest {
        // Given
        val triggering = releaseEvent(attractionId = null)

        val travelOfferCommandHandler = mockk<TravelOfferCommandHandler>(relaxed = true)
        val attractionHandler = mockk<AttractionCommandHandler>(relaxed = true)
        val commuteHandler = mockk<CommuteCommandHandler>(relaxed = true)
        val accommodationHandler = mockk<AccommodationCommandHandler>(relaxed = true)
        val travelOfferService = mockk<TravelOfferService>(relaxed = true)

        val commuteAgg = mockk<Commute>(relaxed = true)
        val accommodationAgg = mockk<Accommodation>(relaxed = true)

        coEvery { commuteHandler.handle(any()) } returns (commuteAgg to listOf(
            CommuteBookingCanceledEvent(
                commuteId = triggering.commuteId,
                bookingId = triggering.bookingId,
                seat = triggering.seat!!,
            )
        ))

        coEvery { accommodationHandler.handle(any()) } returns (accommodationAgg to listOf(
            AccommodationBookingCanceledEvent(
                accommodationId = triggering.accommodationId,
                bookingId = triggering.bookingId,
            )
        ))

        val saga = CancelBookingSaga(
            travelOfferCommandHandler,
            attractionHandler,
            commuteHandler,
            accommodationHandler,
            travelOfferService,
            triggering,
        )

        // When
        saga.execute()

        // Then
        val completed = repo.events.filterIsInstance<BookingCancelSagaCompletedEvent>()
        val failed = repo.events.filterIsInstance<BookingCancelSagaFailedEvent>()
        assertEquals(1, completed.size)
        assertTrue(failed.isEmpty())
        coVerify(exactly = 0) { attractionHandler.handle(any()) }
    }

    @Test
    fun `cancel saga commute failure should publish failed and stop`() = runTest {
        // Given
        val triggering = releaseEvent()

        val travelOfferCommandHandler = mockk<TravelOfferCommandHandler>(relaxed = true)
        val attractionHandler = mockk<AttractionCommandHandler>(relaxed = true)
        val commuteHandler = mockk<CommuteCommandHandler>(relaxed = true)
        val accommodationHandler = mockk<AccommodationCommandHandler>(relaxed = true)
        val travelOfferService = mockk<TravelOfferService>(relaxed = true)

        coEvery { commuteHandler.handle(any()) } throws IllegalStateException("Commute cancel failed")

        val saga = CancelBookingSaga(
            travelOfferCommandHandler,
            attractionHandler,
            commuteHandler,
            accommodationHandler,
            travelOfferService,
            triggering,
        )

        // When
        saga.execute()

        // Then
        val completed = repo.events.filterIsInstance<BookingCancelSagaCompletedEvent>()
        val failed = repo.events.filterIsInstance<BookingCancelSagaFailedEvent>()
        assertTrue(completed.isEmpty())
        assertEquals(1, failed.size)

        coVerify(exactly = 1) { commuteHandler.handle(any()) }
        coVerify(exactly = 0) { accommodationHandler.handle(any()) }
        coVerify(exactly = 0) { attractionHandler.handle(any()) }
    }

    @Test
    fun `cancel saga accommodation failure should publish failed and not call attraction`() = runTest {
        // Given
        val triggering = releaseEvent(attractionId = UUID.randomUUID())

        val travelOfferCommandHandler = mockk<TravelOfferCommandHandler>(relaxed = true)
        val attractionHandler = mockk<AttractionCommandHandler>(relaxed = true)
        val commuteHandler = mockk<CommuteCommandHandler>(relaxed = true)
        val accommodationHandler = mockk<AccommodationCommandHandler>(relaxed = true)
        val travelOfferService = mockk<TravelOfferService>(relaxed = true)

        val commuteAgg = mockk<Commute>(relaxed = true)
        coEvery { commuteHandler.handle(any()) } returns (commuteAgg to listOf(
            CommuteBookingCanceledEvent(
                commuteId = triggering.commuteId,
                bookingId = triggering.bookingId,
                seat = triggering.seat!!,
            )
        ))

        coEvery { accommodationHandler.handle(any()) } throws IllegalStateException("Accommodation cancel failed")

        val saga = CancelBookingSaga(
            travelOfferCommandHandler,
            attractionHandler,
            commuteHandler,
            accommodationHandler,
            travelOfferService,
            triggering,
        )

        // When
        saga.execute()

        // Then
        val completed = repo.events.filterIsInstance<BookingCancelSagaCompletedEvent>()
        val failed = repo.events.filterIsInstance<BookingCancelSagaFailedEvent>()
        assertTrue(completed.isEmpty())
        assertEquals(1, failed.size)

        coVerify(exactly = 1) { commuteHandler.handle(any()) }
        coVerify(exactly = 1) { accommodationHandler.handle(any()) }
        coVerify(exactly = 0) { attractionHandler.handle(any()) }
    }

    @Test
    fun `cancel saga attraction failure should publish failed`() = runTest {
        // Given
        val triggering = releaseEvent(attractionId = UUID.randomUUID())

        val travelOfferCommandHandler = mockk<TravelOfferCommandHandler>(relaxed = true)
        val attractionHandler = mockk<AttractionCommandHandler>(relaxed = true)
        val commuteHandler = mockk<CommuteCommandHandler>(relaxed = true)
        val accommodationHandler = mockk<AccommodationCommandHandler>(relaxed = true)
        val travelOfferService = mockk<TravelOfferService>(relaxed = true)

        val commuteAgg = mockk<Commute>(relaxed = true)
        val accommodationAgg = mockk<Accommodation>(relaxed = true)

        coEvery { commuteHandler.handle(any()) } returns (commuteAgg to listOf(
            CommuteBookingCanceledEvent(
                commuteId = triggering.commuteId,
                bookingId = triggering.bookingId,
                seat = triggering.seat!!,
            )
        ))

        coEvery { accommodationHandler.handle(any()) } returns (accommodationAgg to listOf(
            AccommodationBookingCanceledEvent(
                accommodationId = triggering.accommodationId,
                bookingId = triggering.bookingId,
            )
        ))

        coEvery { attractionHandler.handle(any()) } throws IllegalStateException("Attraction cancel failed")

        val saga = CancelBookingSaga(
            travelOfferCommandHandler,
            attractionHandler,
            commuteHandler,
            accommodationHandler,
            travelOfferService,
            triggering,
        )

        // When
        saga.execute()

        // Then
        val completed = repo.events.filterIsInstance<BookingCancelSagaCompletedEvent>()
        val failed = repo.events.filterIsInstance<BookingCancelSagaFailedEvent>()
        assertTrue(completed.isEmpty())
        assertEquals(1, failed.size)

        coVerify(exactly = 1) { commuteHandler.handle(any()) }
        coVerify(exactly = 1) { accommodationHandler.handle(any()) }
        coVerify(exactly = 1) { attractionHandler.handle(any()) }
    }

    @Test
    fun `cancel saga without attraction and commute failure should publish failed`() = runTest {
        // Given
        val triggering = releaseEvent(attractionId = null)

        val travelOfferCommandHandler = mockk<TravelOfferCommandHandler>(relaxed = true)
        val attractionHandler = mockk<AttractionCommandHandler>(relaxed = true)
        val commuteHandler = mockk<CommuteCommandHandler>(relaxed = true)
        val accommodationHandler = mockk<AccommodationCommandHandler>(relaxed = true)
        val travelOfferService = mockk<TravelOfferService>(relaxed = true)

        coEvery { commuteHandler.handle(any()) } throws IllegalStateException("Commute cancel failed")

        val saga = CancelBookingSaga(
            travelOfferCommandHandler,
            attractionHandler,
            commuteHandler,
            accommodationHandler,
            travelOfferService,
            triggering,
        )

        // When
        saga.execute()

        // Then
        val completed = repo.events.filterIsInstance<BookingCancelSagaCompletedEvent>()
        val failed = repo.events.filterIsInstance<BookingCancelSagaFailedEvent>()
        assertTrue(completed.isEmpty())
        assertEquals(1, failed.size)

        coVerify(exactly = 1) { commuteHandler.handle(any()) }
        coVerify(exactly = 0) { accommodationHandler.handle(any()) }
        coVerify(exactly = 0) { attractionHandler.handle(any()) }
    }
}
