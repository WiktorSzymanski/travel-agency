package pl.szymanski.wiktor.ta.saga

import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import pl.szymanski.wiktor.ta.DummyCommandBus
import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.command.AccommodationCommand
import pl.szymanski.wiktor.ta.command.AttractionCommand
import pl.szymanski.wiktor.ta.command.CancelAccommodationBookingCommand
import pl.szymanski.wiktor.ta.command.CancelAttractionBookingCommand
import pl.szymanski.wiktor.ta.command.CancelCommuteBookingCommand
import pl.szymanski.wiktor.ta.command.CommuteCommand
import pl.szymanski.wiktor.ta.command.CompensateAccommodationCommand
import pl.szymanski.wiktor.ta.command.CompensateAttractionCommand
import pl.szymanski.wiktor.ta.command.CompensateCancelAccommodationBookingCommand
import pl.szymanski.wiktor.ta.command.CompensateCancelAttractionBookingCommand
import pl.szymanski.wiktor.ta.command.CompensateCancelCommuteBookingCommand
import pl.szymanski.wiktor.ta.command.CompensateCommuteCommand
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.AccommodationId
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.aggregate.CommuteId
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.BookingCancelRequestedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.DomainEvent
import pl.szymanski.wiktor.ta.event.BookingCancelSagaCompletedEvent
import pl.szymanski.wiktor.ta.event.BookingCancelSagaFailedEvent
import pl.szymanski.wiktor.ta.event.BookingCancelSagaStartedEvent
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CancelBookingSagaNewTest {
    private lateinit var eventBus: DummyEventBus
    private lateinit var commandBus: DummyCommandBus

    private val metadata = Metadata(UUID.randomUUID(), 1)

    @BeforeTest
    fun setup() {
        eventBus = DummyEventBus()
        commandBus = DummyCommandBus()
    }

    private fun releaseEvent(
        accommodationId: AccommodationId = AccommodationId.generate(),
        commuteId: CommuteId = CommuteId.generate(),
        attractionId: AttractionId = AttractionId.generate(),
        bookingId: BookingId = BookingId.generate(),
        seat: Seat = Seat.Picked("1", "A"),
    ) = BookingCancelRequestedEvent(
        travelOffer = TravelOffer(commuteId, accommodationId, attractionId),
        bookingId = bookingId,
        seat = seat,
    )

    private fun registerCommuteHandler(onCall: (CommuteCommand) -> Pair<Commute, List<DomainEvent>>) {
        commandBus.registerHandler(CommuteCommand::class.java) { c -> onCall(c) }
        commandBus.registerHandler(CompensateCommuteCommand::class.java) { c -> onCall(c) }
    }

    private fun registerAccommodationHandler(onCall: (AccommodationCommand) -> Pair<Accommodation, List<DomainEvent>>) {
        commandBus.registerHandler(AccommodationCommand::class.java) { c -> onCall(c) }
        commandBus.registerHandler(CompensateAccommodationCommand::class.java) { c -> onCall(c) }
    }

    private fun registerAttractionHandler(onCall: (AttractionCommand) -> Pair<Attraction, List<DomainEvent>>) {
        commandBus.registerHandler(AttractionCommand::class.java) { c -> onCall(c) }
        commandBus.registerHandler(CompensateAttractionCommand::class.java) { c -> onCall(c) }
    }

    @Test
    fun `cancel saga success with attraction should complete and publish events`() =
        runTest {
            // Given
            val triggering = releaseEvent(attractionId = AttractionId.generate())

            val commuteAgg = mockk<Commute>(relaxed = true)
            val accommodationAgg = mockk<Accommodation>(relaxed = true)
            val attractionAgg = mockk<Attraction>(relaxed = true)

            registerCommuteHandler { command ->
                when (command) {
                    is CancelCommuteBookingCommand ->
                        commuteAgg to
                            listOf(
                                CommuteBookingCanceledEvent(
                                    commuteId = triggering.travelOffer.commuteId,
                                    bookingId = triggering.bookingId,
                                    seat = triggering.seat,
                                ),
                            )
                    is CompensateCancelCommuteBookingCommand -> error("Should not compensate on success")
                    else -> error("Unexpected commute command: $command")
                }
            }

            registerAccommodationHandler { command ->
                when (command) {
                    is CancelAccommodationBookingCommand ->
                        accommodationAgg to
                            listOf(
                                AccommodationBookingCanceledEvent(
                                    accommodationId = triggering.travelOffer.accommodationId,
                                    bookingId = triggering.bookingId,
                                ),
                            )
                    is CompensateCancelAccommodationBookingCommand -> error("Should not compensate on success")
                    else -> error("Unexpected accommodation command: $command")
                }
            }

            registerAttractionHandler { command ->
                when (command) {
                    is CancelAttractionBookingCommand ->
                        attractionAgg to
                            listOf(
                                AttractionBookingCanceledEvent(
                                    attractionId = triggering.travelOffer.attractionId,
                                    bookingId = triggering.bookingId,
                                ),
                            )
                    is CompensateCancelAttractionBookingCommand -> error("Should not compensate on success")
                    else -> error("Unexpected attraction command: $command")
                }
            }

            val saga = CancelBookingSaga(
                eventBus,
                commandBus,
                triggering.travelOffer,
                triggering.seat,
                triggering.bookingId,
                metadata
            )

            // When
            saga.execute()

            // Then
            val started = eventBus.emittedEvents.map { it.event }.filterIsInstance<BookingCancelSagaStartedEvent>()
            val completed = eventBus.emittedEvents.map { it.event }.filterIsInstance<BookingCancelSagaCompletedEvent>()
            val failed = eventBus.emittedEvents.map { it.event }.filterIsInstance<BookingCancelSagaFailedEvent>()

            assertEquals(1, started.size)
            assertEquals(1, completed.size)
            assertTrue(failed.isEmpty())
            assertEquals(triggering.bookingId, completed.first().bookingId)
        }

    @Test
    fun `cancel saga success without attraction should complete and not call attraction handler`() =
        runTest {
            // Given
            val triggering = releaseEvent(attractionId = AttractionId.Empty)

            val commuteAgg = mockk<Commute>(relaxed = true)
            val accommodationAgg = mockk<Accommodation>(relaxed = true)

            registerCommuteHandler { command ->
                when (command) {
                    is CancelCommuteBookingCommand ->
                        commuteAgg to
                            listOf(
                                CommuteBookingCanceledEvent(
                                    commuteId = triggering.travelOffer.commuteId,
                                    bookingId = triggering.bookingId,
                                    seat = triggering.seat,
                                ),
                            )
                    else -> error("Unexpected commute command: $command")
                }
            }

            var attractionCalled = false
            registerAttractionHandler { command ->
                attractionCalled = true
                error("Attraction should not be called when attractionId is null: $command")
            }

            registerAccommodationHandler { command ->
                when (command) {
                    is CancelAccommodationBookingCommand ->
                        accommodationAgg to
                            listOf(
                                AccommodationBookingCanceledEvent(
                                    accommodationId = triggering.travelOffer.accommodationId,
                                    bookingId = triggering.bookingId,
                                ),
                            )
                    else -> error("Unexpected accommodation command: $command")
                }
            }

            val saga = CancelBookingSaga(
                eventBus,
                commandBus,
                triggering.travelOffer,
                triggering.seat,
                triggering.bookingId,
                metadata
            )
            // When
            saga.execute()

            // Then
            val completed = eventBus.emittedEvents.map { it.event }.filterIsInstance<BookingCancelSagaCompletedEvent>()
            val failed = eventBus.emittedEvents.map { it.event }.filterIsInstance<BookingCancelSagaFailedEvent>()
            assertEquals(1, completed.size)
            assertTrue(failed.isEmpty())
            assertTrue(!attractionCalled)
        }

    @Test
    fun `cancel saga commute failure should publish failed and stop`() =
        runTest {
            // Given
            val triggering = releaseEvent()

            registerCommuteHandler { _ ->
                throw IllegalStateException("Commute cancel failed")
            }
            // Handlers that must not be called
            registerAccommodationHandler { command -> error("Accommodation should not be called: $command") }
            registerAttractionHandler { command -> error("Attraction should not be called: $command") }

            val saga = CancelBookingSaga(
                eventBus,
                commandBus,
                triggering.travelOffer,
                triggering.seat,
                triggering.bookingId,
                metadata
            )
            // When
            saga.execute()

            // Then
            val completed = eventBus.emittedEvents.map { it.event }.filterIsInstance<BookingCancelSagaCompletedEvent>()
            val failed = eventBus.emittedEvents.map { it.event }.filterIsInstance<BookingCancelSagaFailedEvent>()
            assertTrue(completed.isEmpty())
            assertEquals(1, failed.size)
        }

    @Test
    fun `cancel saga accommodation failure should publish failed and not call attraction`() =
        runTest {
            // Given
            val triggering = releaseEvent(attractionId = AttractionId.generate())

            val commuteAgg = mockk<Commute>(relaxed = true)
            registerCommuteHandler { command ->
                when (command) {
                    is CancelCommuteBookingCommand ->
                        commuteAgg to
                            listOf(
                                CommuteBookingCanceledEvent(
                                    commuteId = triggering.travelOffer.commuteId,
                                    bookingId = triggering.bookingId,
                                    seat = triggering.seat,
                                ),
                            )
                    is CompensateCancelCommuteBookingCommand -> mockk<Commute>(relaxed = true) to emptyList()
                    else -> error("Unexpected commute command: $command")
                }
            }

            registerAccommodationHandler { command ->
                when (command) {
                    is CancelAccommodationBookingCommand -> throw IllegalStateException("Accommodation cancel failed")
                    is CompensateCancelAccommodationBookingCommand -> mockk<Accommodation>(relaxed = true) to emptyList()
                    else -> error("Unexpected accommodation command: $command")
                }
            }

            // Attraction should not be called
            registerAttractionHandler { command -> error("Attraction should not be called: $command") }

            val saga = CancelBookingSaga(
                eventBus,
                commandBus,
                triggering.travelOffer,
                triggering.seat,
                triggering.bookingId,
                metadata
            )
            // When
            saga.execute()

            // Then
            val completed = eventBus.emittedEvents.map { it.event }.filterIsInstance<BookingCancelSagaCompletedEvent>()
            val failed = eventBus.emittedEvents.map { it.event }.filterIsInstance<BookingCancelSagaFailedEvent>()
            assertTrue(completed.isEmpty())
            assertEquals(1, failed.size)
        }

    @Test
    fun `cancel saga attraction failure should publish failed`() =
        runTest {
            // Given
            val triggering = releaseEvent(attractionId = AttractionId.generate())

            val commuteAgg = mockk<Commute>(relaxed = true)
            val accommodationAgg = mockk<Accommodation>(relaxed = true)

            registerCommuteHandler { command ->
                when (command) {
                    is CancelCommuteBookingCommand ->
                        commuteAgg to
                            listOf(
                                CommuteBookingCanceledEvent(
                                    commuteId = triggering.travelOffer.commuteId,
                                    bookingId = triggering.bookingId,
                                    seat = triggering.seat,
                                ),
                            )
                    is CompensateCancelCommuteBookingCommand -> mockk<Commute>(relaxed = true) to emptyList()
                    else -> error("Unexpected commute command: $command")
                }
            }

            registerAccommodationHandler { command ->
                when (command) {
                    is CancelAccommodationBookingCommand ->
                        accommodationAgg to
                            listOf(
                                AccommodationBookingCanceledEvent(
                                    accommodationId = triggering.travelOffer.accommodationId,
                                    bookingId = triggering.bookingId,
                                ),
                            )
                    is CompensateCancelAccommodationBookingCommand -> mockk<Accommodation>(relaxed = true) to emptyList()
                    else -> error("Unexpected accommodation command: $command")
                }
            }

            registerAttractionHandler { command ->
                when (command) {
                    is CancelAttractionBookingCommand -> throw IllegalStateException("Attraction cancel failed")
                    is CompensateCancelAttractionBookingCommand -> mockk<Attraction>(relaxed = true) to emptyList()
                    else -> error("Unexpected attraction command: $command")
                }
            }

            val saga = CancelBookingSaga(
                eventBus,
                commandBus,
                triggering.travelOffer,
                triggering.seat,
                triggering.bookingId,
                metadata
            )
            // When
            saga.execute()

            // Then
            val completed = eventBus.emittedEvents.map { it.event }.filterIsInstance<BookingCancelSagaCompletedEvent>()
            val failed = eventBus.emittedEvents.map { it.event }.filterIsInstance<BookingCancelSagaFailedEvent>()
            assertTrue(completed.isEmpty())
            assertEquals(1, failed.size)
        }

    @Test
    fun `cancel saga without attraction and commute failure should publish failed`() =
        runTest {
            // Given
            val triggering = releaseEvent(attractionId = AttractionId.Empty)

            registerCommuteHandler { _ -> throw IllegalStateException("Commute cancel failed") }
            // Handlers that must not be called
            registerAccommodationHandler { command -> error("Accommodation should not be called: $command") }
            registerAttractionHandler { command -> error("Attraction should not be called: $command") }

            val saga = CancelBookingSaga(
                eventBus,
                commandBus,
                triggering.travelOffer,
                triggering.seat,
                triggering.bookingId,
                metadata
            )
            // When
            saga.execute()

            // Then
            val completed = eventBus.emittedEvents.map { it.event }.filterIsInstance<BookingCancelSagaCompletedEvent>()
            val failed = eventBus.emittedEvents.map { it.event }.filterIsInstance<BookingCancelSagaFailedEvent>()
            assertTrue(completed.isEmpty())
            assertEquals(1, failed.size)
        }
}
