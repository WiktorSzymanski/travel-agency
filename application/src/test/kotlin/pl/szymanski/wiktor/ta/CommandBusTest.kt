package pl.szymanski.wiktor.ta

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import pl.szymanski.wiktor.ta.command.*
import pl.szymanski.wiktor.ta.commandhandler.*
import pl.szymanski.wiktor.ta.domain.LocationAndTime
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.Rent
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.aggregate.*
import pl.szymanski.wiktor.ta.domain.event.AccommodationEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionEvent
import pl.szymanski.wiktor.ta.domain.event.BookingEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteEvent
import pl.szymanski.wiktor.ta.domain.event.ProcessBookingEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferEvent
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CommandBusTest {
    private lateinit var commandBus: DummyCommandBus

    @BeforeTest
    fun setup() {
        commandBus = DummyCommandBus()
    }

    @Test
    fun `dispatch should throw if no handler registered for command superclass`() =
        runTest {
            // Given
            commandBus.registerHandler(BookingCommand::class.java) { command: BookingCommand ->
                assertIs<CreateBookingCommand>(command)
                "OK" to listOf(
                    ProcessBookingEvent(
                        bookingId = BookingId.generate(),
                    ),
                )
            }

            val command =
                CreateAccommodationCommand(
                    accommodationId = AccommodationId.generate(),
                    correlationId = UUID.randomUUID(),
                    name = "Hotel",
                    location = LocationEnum.PARIS,
                    rent = Rent(LocalDateTime.now(), LocalDateTime.now().plusDays(1)),
                )

            // When/Then
            assertFailsWith<IllegalArgumentException> { commandBus.dispatch<AccommodationCommand, Any>(command) }
        }

    @Test
    fun `registerHandler should enable dispatch for subclass using superclass key`() =
        runTest {
            // Given
            commandBus.registerHandler(BookingCommand::class.java) { command: BookingCommand ->
                assertIs<CreateBookingCommand>(command)
                "OK" to listOf(
                    ProcessBookingEvent(
                        bookingId = BookingId.generate(),
                    ),
                )
            }

            val command =
                CreateBookingCommand(
                    bookingId = BookingId.generate(),
                    correlationId = UUID.randomUUID(),
                    travelOfferId = TravelOfferId.from(UUID.randomUUID()),
                    userId = UUID.randomUUID(),
                    seat = Seat.Any,
                )

            // When
            val (result, events) = commandBus.dispatch<BookingCommand, String>(command)

            // Then
            assertEquals("OK", result)
            assertEquals(1, events.size)
        }

    @Test
    fun `setup should wire handlers for all command groups and dispatch correctly`() =
        runTest {
            // Given
            val travelOfferHandler = mockk<TravelOfferCommandHandler>()
            val bookingHandler = mockk<BookingCommandHandler>()
            val commuteHandler = mockk<CommuteCommandHandler>()
            val attractionHandler = mockk<AttractionCommandHandler>()
            val accommodationHandler = mockk<AccommodationCommandHandler>()

            val travelOffer = mockk<TravelOffer>(relaxed = true)
            val booking = mockk<Booking>(relaxed = true)
            val commute = mockk<Commute>(relaxed = true)
            val attraction = mockk<Attraction>(relaxed = true)
            val accommodation = mockk<Accommodation>(relaxed = true)

            coEvery { travelOfferHandler.handle(any()) } returns (travelOffer to emptyList<TravelOfferEvent>())
            coEvery { bookingHandler.handle(any()) } returns (booking to emptyList<BookingEvent>())
            coEvery { commuteHandler.handle(any()) } returns (commute to emptyList<CommuteEvent>())
            coEvery { attractionHandler.handle(any()) } returns (attraction to emptyList<AttractionEvent>())
            coEvery { accommodationHandler.handle(any()) } returns (accommodation to emptyList<AccommodationEvent>())

            commandBus = DummyCommandBus(
                travelOfferHandler,
                bookingHandler,
                commuteHandler,
                attractionHandler,
                accommodationHandler,
            )

            val correlationId = UUID.randomUUID()

            val bookingId = BookingId.generate()
            val accommodationId = AccommodationId.generate()
            val attractionId = AttractionId.generate()
            val commuteId = CommuteId.generate()
            val travelOfferId = TravelOfferId.generate(commuteId, accommodationId, attractionId)

            // When
            val (_, toEvents) =
                commandBus.dispatch<TravelOfferCommand, TravelOffer>(
                    CreateTravelOfferCommand(
                        travelOfferId = travelOfferId,
                        correlationId = correlationId,
                        name = "Trip",
                        commuteId = commuteId,
                        accommodationId = accommodationId,
                        attractionId = attractionId,
                    ),
                )
            val (_, bEvents) =
                commandBus.dispatch<BookingCommand, Booking>(
                    CreateBookingCommand(
                        bookingId = bookingId,
                        correlationId = correlationId,
                        travelOfferId = travelOfferId,
                        userId = UUID.randomUUID(),
                        seat = Seat.Any,
                    ),
                )
            val (_, cEvents) =
                commandBus.dispatch<CommuteCommand, Commute>(
                    CreateCommuteCommand(
                        commuteId = commuteId,
                        correlationId = correlationId,
                        name = "Bus",
                        departure = LocationAndTime(LocationEnum.PARIS, LocalDateTime.now()),
                        arrival = LocationAndTime(LocationEnum.LONDON, LocalDateTime.now().plusHours(1)),
                        seats = listOf(Seat.Picked("1", "A")),
                    ),
                )
            val (_, aEvents) =
                commandBus.dispatch<AttractionCommand, Attraction>(
                    CreateAttractionCommand(
                        attractionId = attractionId,
                        correlationId = correlationId,
                        name = "Museum",
                        location = LocationEnum.PARIS,
                        date = LocalDateTime.now().plusDays(1),
                        capacity = 10,
                    ),
                )
            val (_, accEvents) =
                commandBus.dispatch<AccommodationCommand, Accommodation>(
                    CreateAccommodationCommand(
                        accommodationId = accommodationId,
                        correlationId = correlationId,
                        name = "Hotel",
                        location = LocationEnum.PARIS,
                        rent = Rent(LocalDateTime.now(), LocalDateTime.now().plusDays(7)),
                    ),
                )

            // Then
            coVerify(exactly = 1) { travelOfferHandler.handle(any()) }
            coVerify(exactly = 1) { bookingHandler.handle(any()) }
            coVerify(exactly = 1) { commuteHandler.handle(any()) }
            coVerify(exactly = 1) { attractionHandler.handle(any()) }
            coVerify(exactly = 1) { accommodationHandler.handle(any()) }

            assertTrue(toEvents.isEmpty())
            assertTrue(bEvents.isEmpty())
            assertTrue(cEvents.isEmpty())
            assertTrue(aEvents.isEmpty())
            assertTrue(accEvents.isEmpty())
        }
}
