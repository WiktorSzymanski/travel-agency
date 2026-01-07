package pl.szymanski.wiktor.ta.saga

import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import pl.szymanski.wiktor.ta.DummyCommandBus
import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.command.AccommodationCommand
import pl.szymanski.wiktor.ta.command.AttractionCommand
import pl.szymanski.wiktor.ta.command.BookAccommodationCommand
import pl.szymanski.wiktor.ta.command.BookAttractionCommand
import pl.szymanski.wiktor.ta.command.BookCommuteCommand
import pl.szymanski.wiktor.ta.command.CommuteCommand
import pl.szymanski.wiktor.ta.command.CompensateAccommodationCommand
import pl.szymanski.wiktor.ta.command.CompensateBookAccommodationCommand
import pl.szymanski.wiktor.ta.command.CompensateBookAttractionCommand
import pl.szymanski.wiktor.ta.command.CompensateBookCommuteCommand
import pl.szymanski.wiktor.ta.command.CompensateCommuteCommand
import pl.szymanski.wiktor.ta.domain.BookingState
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.AccommodationId
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.aggregate.CommuteId
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookedEvent
import pl.szymanski.wiktor.ta.domain.event.BookingCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookedEvent
import pl.szymanski.wiktor.ta.domain.event.DomainEvent
import pl.szymanski.wiktor.ta.event.BookingSagaCompletedEvent
import pl.szymanski.wiktor.ta.event.BookingSagaFailedEvent
import pl.szymanski.wiktor.ta.event.BookingSagaStartedEvent
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BookingSagaNewTest {
    private lateinit var eventBus: DummyEventBus
    private lateinit var commandBus: DummyCommandBus

    private val metadata = Metadata(UUID.randomUUID(), 1)

    @BeforeTest
    fun setup() {
        eventBus = DummyEventBus()
        commandBus = DummyCommandBus()
    }

    private fun reservedEvent(
        accommodationId: AccommodationId = AccommodationId.generate(),
        commuteId: CommuteId = CommuteId.generate(),
        attractionId: AttractionId = AttractionId.generate(),
        bookingId: BookingId = BookingId.generate(),
        seat: Seat = Seat.Picked("1", "A"),
    ) = BookingCreatedEvent(
        travelOffer = TravelOffer(commuteId, accommodationId, attractionId),
        bookingId = bookingId,
        seat = seat,
        userId = UUID.randomUUID(),
        state = BookingState.NEW,
    )

    private fun registerCommuteHandler(onCall: (CommuteCommand) -> Pair<Commute, List<DomainEvent>>) {
        commandBus.registerHandler(CommuteCommand::class.java) { c -> onCall(c) }
        // Register also for the immediate superclass used by CommandBus for compensation commands
        commandBus.registerHandler(CompensateCommuteCommand::class.java) { c -> onCall(c) }
    }

    private fun registerAccommodationHandler(onCall: (AccommodationCommand) -> Pair<Accommodation, List<DomainEvent>>) {
        commandBus.registerHandler(AccommodationCommand::class.java) { c -> onCall(c) }
        // Register also for the immediate superclass used by CommandBus for compensation commands
        commandBus.registerHandler(CompensateAccommodationCommand::class.java) { c -> onCall(c) }
    }

    private fun registerAttractionHandler(onCall: (AttractionCommand) -> Pair<Attraction, List<DomainEvent>>) {
        commandBus.registerHandler(AttractionCommand::class.java) { c -> onCall(c) }
    }

    @Test
    fun `saga success with attraction should complete and publish events`() =
        runTest {
            // Given
            val commuteAgg = mockk<Commute>(relaxed = true)
            val accommodationAgg = mockk<Accommodation>(relaxed = true)
            val attractionAgg = mockk<Attraction>(relaxed = true)

            val triggering = reservedEvent(attractionId = AttractionId.generate())

            registerCommuteHandler { command ->
                when (command) {
                    is BookCommuteCommand ->
                        commuteAgg to
                            listOf(
                                CommuteBookedEvent(
                                    commuteId = command.commuteId,
                                    bookingId = command.bookingId,
                                    seat = command.seat,
                                ),
                            )
                    is CompensateBookCommuteCommand -> error("Should not compensate on success")
                    else -> error("Unexpected commute command: $command")
                }
            }

            registerAccommodationHandler { command ->
                when (command) {
                    is BookAccommodationCommand ->
                        accommodationAgg to
                            listOf(
                                AccommodationBookedEvent(
                                    accommodationId = command.accommodationId,
                                    bookingId = command.bookingId,
                                ),
                            )
                    is CompensateBookAccommodationCommand -> error("Should not compensate on success")
                    else -> error("Unexpected accommodation command: $command")
                }
            }

            registerAttractionHandler { command ->
                when (command) {
                    is BookAttractionCommand ->
                        attractionAgg to
                            listOf(
                                AttractionBookedEvent(
                                    attractionId = command.attractionId,
                                    bookingId = command.bookingId,
                                ),
                            )
                    is CompensateBookAttractionCommand -> error("Should not compensate on success")
                    else -> error("Unexpected attraction command: $command")
                }
            }

            val saga = BookingSaga(
                eventBus,
                commandBus,
                triggering.travelOffer,
                triggering.seat,
                triggering.bookingId,
                metadata)

            // When
            saga.execute()

            // Then
            val started = eventBus.emittedEvents.map { it.event }.filterIsInstance<BookingSagaStartedEvent>()
            val completed = eventBus.emittedEvents.map { it.event }.filterIsInstance<BookingSagaCompletedEvent>()
            val failed = eventBus.emittedEvents.map { it.event }.filterIsInstance<BookingSagaFailedEvent>()

            assertEquals(1, started.size)
            assertEquals(1, completed.size)
            assertTrue(failed.isEmpty())
            assertEquals(triggering.bookingId, completed.first().bookingId)
        }

    @Test
    fun `saga success without attraction should complete and not call attraction handler`() =
        runTest {
            // Given
            val commuteAgg = mockk<Commute>(relaxed = true)
            val accommodationAgg = mockk<Accommodation>(relaxed = true)
            var attractionCalled = false

            val triggering = reservedEvent(attractionId = AttractionId.Empty)

            registerCommuteHandler { command ->
                when (command) {
                    is BookCommuteCommand ->
                        commuteAgg to
                            listOf(
                                CommuteBookedEvent(
                                    commuteId = command.commuteId,
                                    bookingId = command.bookingId,
                                    seat = command.seat,
                                ),
                            )
                    else -> error("Unexpected commute command: $command")
                }
            }

            registerAccommodationHandler { command ->
                when (command) {
                    is BookAccommodationCommand ->
                        accommodationAgg to
                            listOf(
                                AccommodationBookedEvent(
                                    accommodationId = command.accommodationId,
                                    bookingId = command.bookingId,
                                ),
                            )
                    else -> error("Unexpected accommodation command: $command")
                }
            }

            registerAttractionHandler { command ->
                attractionCalled = true
                error("Attraction handler should not be called when attractionId is null, but got: $command")
            }

            val saga = BookingSaga(
                eventBus,
                commandBus,
                triggering.travelOffer,
                triggering.seat,
                triggering.bookingId,
                metadata)

            // When
            saga.execute()

            // Then
            val completed = eventBus.emittedEvents.map { it.event }.filterIsInstance<BookingSagaCompletedEvent>()
            val failed = eventBus.emittedEvents.map { it.event }.filterIsInstance<BookingSagaFailedEvent>()
            assertEquals(1, completed.size)
            assertTrue(failed.isEmpty())
            assertTrue(!attractionCalled)
        }

    @Test
    fun `saga commute failure should publish failed and stop`() =
        runTest {
            // Given
            val triggering = reservedEvent()

            registerCommuteHandler { _ ->
                throw IllegalStateException("Commute booking failed")
            }

            // Handlers that must not be called
            registerAccommodationHandler { command -> error("Accommodation should not be called: $command") }
            registerAttractionHandler { command -> error("Attraction should not be called: $command") }

            val saga = BookingSaga(
                eventBus,
                commandBus,
                triggering.travelOffer,
                triggering.seat,
                triggering.bookingId,
                metadata)

            // When
            saga.execute()

            // Then
            val completed = eventBus.emittedEvents.map { it.event }.filterIsInstance<BookingSagaCompletedEvent>()
            val failed = eventBus.emittedEvents.map { it.event }.filterIsInstance<BookingSagaFailedEvent>()
            assertTrue(completed.isEmpty())
            assertEquals(1, failed.size)
            assertEquals(triggering.bookingId, failed.first().bookingId)
        }

    @Test
    fun `saga commute failure without attraction should publish failed and stop`() =
        runTest {
            // Given
            val triggering = reservedEvent(attractionId = AttractionId.Empty)

            registerCommuteHandler { _ ->
                throw IllegalStateException("Commute booking failed")
            }

            // Handlers that must not be called
            registerAccommodationHandler { command -> error("Accommodation should not be called: $command") }
            registerAttractionHandler { command -> error("Attraction should not be called: $command") }

            val saga = BookingSaga(
                eventBus,
                commandBus,
                triggering.travelOffer,
                triggering.seat,
                triggering.bookingId,
                metadata)
            // When
            saga.execute()

            // Then
            val completed = eventBus.emittedEvents.map { it.event }.filterIsInstance<BookingSagaCompletedEvent>()
            val failed = eventBus.emittedEvents.map { it.event }.filterIsInstance<BookingSagaFailedEvent>()
            assertTrue(completed.isEmpty())
            assertEquals(1, failed.size)
            assertEquals(triggering.bookingId, failed.first().bookingId)
        }

    @Test
    fun `saga accommodation failure should compensate commute and publish failed`() =
        runTest {
            // Given
            val commuteAgg = mockk<Commute>(relaxed = true)
            var commuteCompensated = false

            val triggering = reservedEvent(attractionId = AttractionId.Empty)

            lateinit var commuteBookedEvent: CommuteBookedEvent

            registerCommuteHandler { command ->
                when (command) {
                    is BookCommuteCommand -> {
                        commuteBookedEvent =
                            CommuteBookedEvent(
                                commuteId = command.commuteId,
                                bookingId = command.bookingId,
                                seat = command.seat
                            )
                        commuteAgg to listOf(commuteBookedEvent)
                    }
                    is CompensateBookCommuteCommand -> {
                        // Validate eventId matches earlier event
                        assertEquals(commuteBookedEvent.eventId, command.eventId)
                        commuteCompensated = true
                        commuteAgg to emptyList()
                    }
                    else -> error("Unexpected commute command: $command")
                }
            }

            registerAccommodationHandler { command ->
                when (command) {
                    is BookAccommodationCommand -> throw IllegalStateException("Accommodation booking failed")
                    else -> error("Unexpected accommodation command: $command")
                }
            }

            // Attraction should not be called
            registerAttractionHandler { command -> error("Attraction should not be called: $command") }

            val saga = BookingSaga(
                eventBus,
                commandBus,
                triggering.travelOffer,
                triggering.seat,
                triggering.bookingId,
                metadata)

            // When
            saga.execute()

            // Then
            val completed = eventBus.emittedEvents.map { it.event }.filterIsInstance<BookingSagaCompletedEvent>()
            val failed = eventBus.emittedEvents.map { it.event }.filterIsInstance<BookingSagaFailedEvent>()
            assertTrue(completed.isEmpty())
            assertEquals(1, failed.size)
            assertTrue(commuteCompensated)
        }

    @Test
    fun `saga attraction failure should compensate accommodation and commute and publish failed`() =
        runTest {
            // Given
            val commuteAgg = mockk<Commute>(relaxed = true)
            val accommodationAgg = mockk<Accommodation>(relaxed = true)

            var commuteCompensated = false
            var accommodationCompensated = false

            val triggering = reservedEvent(attractionId = AttractionId.generate())

            lateinit var commuteBookedEvent: CommuteBookedEvent
            lateinit var accommodationBookedEvent: AccommodationBookedEvent

            registerCommuteHandler { command ->
                when (command) {
                    is BookCommuteCommand -> {
                        commuteBookedEvent =
                            CommuteBookedEvent(
                                commuteId = command.commuteId,
                                bookingId = command.bookingId,
                                seat = command.seat
                            )
                        commuteAgg to listOf(commuteBookedEvent)
                    }
                    is CompensateBookCommuteCommand -> {
                        assertEquals(commuteBookedEvent.eventId, command.eventId)
                        commuteCompensated = true
                        commuteAgg to emptyList()
                    }
                    else -> error("Unexpected commute command: $command")
                }
            }

            registerAccommodationHandler { command ->
                when (command) {
                    is BookAccommodationCommand -> {
                        accommodationBookedEvent =
                            AccommodationBookedEvent(
                                accommodationId = command.accommodationId,
                                bookingId = command.bookingId,
                            )
                        accommodationAgg to listOf(accommodationBookedEvent)
                    }
                    is CompensateBookAccommodationCommand -> {
                        assertEquals(accommodationBookedEvent.eventId, command.eventId)
                        accommodationCompensated = true
                        accommodationAgg to emptyList()
                    }
                    else -> error("Unexpected accommodation command: $command")
                }
            }

            registerAttractionHandler { command ->
                when (command) {
                    is BookAttractionCommand -> throw IllegalStateException("Attraction booking failed")
                    else -> error("Unexpected attraction command: $command")
                }
            }

            val saga = BookingSaga(
                eventBus,
                commandBus,
                triggering.travelOffer,
                triggering.seat,
                triggering.bookingId,
                metadata)

            // When
            saga.execute()

            // Then
            val completed = eventBus.emittedEvents.map { it.event }.filterIsInstance<BookingSagaCompletedEvent>()
            val failed = eventBus.emittedEvents.map { it.event }.filterIsInstance<BookingSagaFailedEvent>()
            assertTrue(completed.isEmpty())
            assertEquals(1, failed.size)
            assertTrue(accommodationCompensated)
            assertTrue(commuteCompensated)
        }
}
