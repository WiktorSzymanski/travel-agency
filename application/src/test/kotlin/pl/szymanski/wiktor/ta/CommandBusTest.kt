//package pl.szymanski.wiktor.ta
//
//import io.mockk.coEvery
//import io.mockk.coVerify
//import io.mockk.mockk
//import kotlinx.coroutines.test.runTest
//import pl.szymanski.wiktor.ta.command.AccommodationCommand
//import pl.szymanski.wiktor.ta.command.AttractionCommand
//import pl.szymanski.wiktor.ta.command.BookingCommand
//import pl.szymanski.wiktor.ta.command.CommuteCommand
//import pl.szymanski.wiktor.ta.command.CreateAccommodationCommand
//import pl.szymanski.wiktor.ta.command.CreateAttractionCommand
//import pl.szymanski.wiktor.ta.command.CreateBookingCommand
//import pl.szymanski.wiktor.ta.command.CreateCommuteCommand
//import pl.szymanski.wiktor.ta.commandhandler.AccommodationCommandHandler
//import pl.szymanski.wiktor.ta.commandhandler.AttractionCommandHandler
//import pl.szymanski.wiktor.ta.commandhandler.BookingCommandHandler
//import pl.szymanski.wiktor.ta.commandhandler.CommuteCommandHandler
//import pl.szymanski.wiktor.ta.domain.LocationAndTime
//import pl.szymanski.wiktor.ta.domain.LocationEnum
//import pl.szymanski.wiktor.ta.domain.Rent
//import pl.szymanski.wiktor.ta.domain.Seat
//import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
//import pl.szymanski.wiktor.ta.domain.aggregate.AccommodationId
//import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
//import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
//import pl.szymanski.wiktor.ta.domain.aggregate.Booking
//import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
//import pl.szymanski.wiktor.ta.domain.aggregate.Commute
//import pl.szymanski.wiktor.ta.domain.aggregate.CommuteId
//import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
//import pl.szymanski.wiktor.ta.domain.event.AccommodationEvent
//import pl.szymanski.wiktor.ta.domain.event.AttractionEvent
//import pl.szymanski.wiktor.ta.domain.event.BookingEvent
//import pl.szymanski.wiktor.ta.domain.event.CommuteEvent
//import pl.szymanski.wiktor.ta.domain.event.ProcessBookingEvent
//import java.time.LocalDateTime
//import java.util.UUID
//import kotlin.test.BeforeTest
//import kotlin.test.Test
//import kotlin.test.assertEquals
//import kotlin.test.assertFailsWith
//import kotlin.test.assertIs
//import kotlin.test.assertTrue
//
//class CommandBusTest {
//    private lateinit var commandBus: DummyCommandBus
//
//    @BeforeTest
//    fun setup() {
//        commandBus = DummyCommandBus()
//    }
//
//    @Test
//    fun `dispatch should throw if no handler registered for command superclass`() =
//        runTest {
//            // Given
//            commandBus.registerHandler<BookingCommand, Booking>(BookingCommand::class.java) { command: BookingCommand ->
//                assertIs<CreateBookingCommand>(command)
//                Triple(
//                    "OK",
//                    listOf(
//                        ProcessBookingEvent(
//                            bookingId = BookingId.generate(),
//                        ),
//                    ),
//                    Metadata(command.correlationId, 0L)
//                )
//            }
//
//            val correlationId = UUID.randomUUID()
//            val command =
//                CreateAccommodationCommand(
//                    accommodationId = AccommodationId.generate(),
//                    correlationId = correlationId,
//                    name = "Hotel",
//                    location = LocationEnum.PARIS,
//                    rent = Rent(LocalDateTime.now(), LocalDateTime.now().plusDays(1)),
//                )
//
//            // When/Then
//            assertFailsWith<IllegalArgumentException> { commandBus.dispatch<AccommodationCommand, Any>(command) }
//        }
//
//    @Test
//    fun `registerHandler should enable dispatch for subclass using superclass key`() =
//        runTest {
//            // Given
//            commandBus.registerHandler<BookingCommand, Booking>(BookingCommand::class.java) { command: BookingCommand ->
//                assertIs<CreateBookingCommand>(command)
//                Triple(
//                    "OK",
//                    listOf(
//                        ProcessBookingEvent(
//                            bookingId = BookingId.generate(),
//                        ),
//                    ),
//                    Metadata(command.correlationId, 0L)
//                )
//            }
//
//            val correlationId = UUID.randomUUID()
//            val command =
//                CreateBookingCommand(
//                    bookingId = BookingId.generate(),
//                    correlationId = correlationId,
//                    travelOffer = TravelOffer(
//                        CommuteId.generate(),
//                        AccommodationId.generate(),
//                    ),
//                    userId = UUID.randomUUID(),
//                    seat = Seat.Any,
//                )
//
//            // When
////            val (result, events, metadata) = commandBus.dispatch<BookingCommand, String>(command)
//
////            // Then
////            assertEquals("OK", result)
////            assertEquals(1, events.size)
////            assertEquals(correlationId, metadata.correlationId)
////            assertEquals(0L, metadata.revision)
//        }
//
//    @Test
//    fun `setup should wire handlers for all command groups and dispatch correctly`() =
//        runTest {
//            // Given
//            val bookingHandler = mockk<BookingCommandHandler>()
//            val commuteHandler = mockk<CommuteCommandHandler>()
//            val attractionHandler = mockk<AttractionCommandHandler>()
//            val accommodationHandler = mockk<AccommodationCommandHandler>()
//
//            val travelOffer = mockk<TravelOffer>(relaxed = true)
//            val booking = mockk<Booking>(relaxed = true)
//            val commute = mockk<Commute>(relaxed = true)
//            val attraction = mockk<Attraction>(relaxed = true)
//            val accommodation = mockk<Accommodation>(relaxed = true)
//
//            val metadata = mockk<Metadata>(relaxed = true)
//
//            coEvery { bookingHandler.handle(any()) } returns Triple(booking, emptyList<BookingEvent>(), metadata)
//            coEvery { commuteHandler.handle(any()) } returns Triple(commute, emptyList<CommuteEvent>(), metadata)
//            coEvery { attractionHandler.handle(any()) } returns Triple(attraction, emptyList<AttractionEvent>(), metadata)
//            coEvery { accommodationHandler.handle(any()) } returns Triple(accommodation, emptyList<AccommodationEvent>(), metadata)
//
//            commandBus = DummyCommandBus(
//                bookingHandler,
//                commuteHandler,
//                attractionHandler,
//                accommodationHandler,
//            )
//
//            val correlationId = UUID.randomUUID()
//
//            val bookingId = BookingId.generate()
//            val accommodationId = AccommodationId.generate()
//            val attractionId = AttractionId.generate()
//            val commuteId = CommuteId.generate()
//
//            // When
//            val (_, bEvents, _) =
//                commandBus.dispatch<BookingCommand, Booking>(
//                    CreateBookingCommand(
//                        bookingId = bookingId,
//                        correlationId = correlationId,
//                        travelOffer = travelOffer,
//                        userId = UUID.randomUUID(),
//                        seat = Seat.Any,
//                    ),
//                )
//            val (_, cEvents, _) =
//                commandBus.dispatch<CommuteCommand, Commute>(
//                    CreateCommuteCommand(
//                        commuteId = commuteId,
//                        correlationId = correlationId,
//                        name = "Bus",
//                        departure = LocationAndTime(LocationEnum.PARIS, LocalDateTime.now()),
//                        arrival = LocationAndTime(LocationEnum.LONDON, LocalDateTime.now().plusHours(1)),
//                        seats = listOf(Seat.Picked("1", "A")),
//                    ),
//                )
//            val (_, aEvents, _) =
//                commandBus.dispatch<AttractionCommand, Attraction>(
//                    CreateAttractionCommand(
//                        attractionId = attractionId,
//                        correlationId = correlationId,
//                        name = "Museum",
//                        location = LocationEnum.PARIS,
//                        date = LocalDateTime.now().plusDays(1),
//                        capacity = 10,
//                    ),
//                )
//            val (_, accEvents, _) =
//                commandBus.dispatch<AccommodationCommand, Accommodation>(
//                    CreateAccommodationCommand(
//                        accommodationId = accommodationId,
//                        correlationId = correlationId,
//                        name = "Hotel",
//                        location = LocationEnum.PARIS,
//                        rent = Rent(LocalDateTime.now(), LocalDateTime.now().plusDays(7)),
//                    ),
//                )
//
//            // Then
//            coVerify(exactly = 1) { bookingHandler.handle(any()) }
//            coVerify(exactly = 1) { commuteHandler.handle(any()) }
//            coEvery { attractionHandler.handle(any()) }
//            coVerify(exactly = 1) { accommodationHandler.handle(any()) }
//
//            assertTrue(bEvents.isEmpty())
//            assertTrue(cEvents.isEmpty())
//            assertTrue(aEvents.isEmpty())
//            assertTrue(accEvents.isEmpty())
//        }
//}
