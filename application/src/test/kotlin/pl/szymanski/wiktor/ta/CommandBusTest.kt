package pl.szymanski.wiktor.ta

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import pl.szymanski.wiktor.ta.command.*
import pl.szymanski.wiktor.ta.commandHandler.*
import pl.szymanski.wiktor.ta.domain.LocationAndTime
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.Rent
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.aggregate.*
import pl.szymanski.wiktor.ta.domain.event.AccommodationEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionEvent
import pl.szymanski.wiktor.ta.domain.event.BookingEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteEvent
import pl.szymanski.wiktor.ta.domain.event.Event
import pl.szymanski.wiktor.ta.domain.event.TravelOfferEvent
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Unit tests for CommandBus covering registration, dispatching and setup wiring.
 */
class CommandBusTest {
    // Simple sealed command hierarchy to validate superclass based lookup
    sealed class ADummyCommand : Command {
        abstract val id: UUID
        override val correlationId: UUID = UUID.randomUUID()
    }

    data class OperationOfADummyCommand(override val id: UUID = UUID.randomUUID()) : ADummyCommand()

    data class DummyEvent(override val eventId: UUID = UUID.randomUUID(), override var correlationId: UUID? = null) : Event

    // Separate sealed class to avoid cross-test interference with registered handlers
    sealed class BDummyCommand : Command {
        abstract val id: UUID
        override val correlationId: UUID = UUID.randomUUID()
    }

    data class OperationOfBDummyCommand(override val id: UUID = UUID.randomUUID()) : BDummyCommand()

    @Test
    fun `dispatch should throw if no handler registered for command superclass`() =
        runTest {
            // Given
            CommandBus.registerHandler(ADummyCommand::class.java) { command: ADummyCommand ->
                assertIs<OperationOfADummyCommand>(command)
                "OK" to listOf(DummyEvent(correlationId = command.correlationId))
            }

            val command = OperationOfBDummyCommand()

            // When/Then
            assertFailsWith<IllegalArgumentException> { CommandBus.dispatch<BDummyCommand, Any>(command) }
        }

    @Test
    fun `registerHandler should enable dispatch for subclass using superclass key`() =
        runTest {
            // Given
            CommandBus.registerHandler(ADummyCommand::class.java) { command: ADummyCommand ->
                assertIs<OperationOfADummyCommand>(command)
                "OK" to listOf(DummyEvent(correlationId = command.correlationId))
            }

            val command = OperationOfADummyCommand()

            // When
            val (result, events) = CommandBus.dispatch<ADummyCommand, String>(command)

            // Then
            assertEquals("OK", result)
            assertEquals(1, events.size)
            assertEquals(command.correlationId, events.first().correlationId)
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

            CommandBus.setup(
                travelOfferHandler,
                bookingHandler,
                commuteHandler,
                attractionHandler,
                accommodationHandler,
            )

            val correlationId = UUID.randomUUID()
            val travelOfferId = UUID.randomUUID()
            val bookingId = UUID.randomUUID()
            val accommodationId = UUID.randomUUID()
            val attractionId = UUID.randomUUID()
            val commuteId = UUID.randomUUID()

            // When
            val (_, toEvents) =
                CommandBus.dispatch<TravelOfferCommand, TravelOffer>(
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
                CommandBus.dispatch<BookingCommand, Booking>(
                    CreateBookingCommand(
                        bookingId = bookingId,
                        correlationId = correlationId,
                        travelOfferId = travelOfferId,
                        userId = UUID.randomUUID(),
                        seat = null,
                    ),
                )
            val (_, cEvents) =
                CommandBus.dispatch<CommuteCommand, Commute>(
                    CreateCommuteCommand(
                        commuteId = commuteId,
                        correlationId = correlationId,
                        name = "Bus",
                        departure = LocationAndTime(LocationEnum.PARIS, LocalDateTime.now()),
                        arrival = LocationAndTime(LocationEnum.LONDON, LocalDateTime.now().plusHours(1)),
                        seats = listOf(Seat("1", "A")),
                    ),
                )
            val (_, aEvents) =
                CommandBus.dispatch<AttractionCommand, Attraction>(
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
                CommandBus.dispatch<AccommodationCommand, Accommodation>(
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
