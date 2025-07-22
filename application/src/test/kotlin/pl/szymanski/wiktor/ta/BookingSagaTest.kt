package pl.szymanski.wiktor.ta

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import pl.szymanski.wiktor.ta.command.AccommodationCommand
import pl.szymanski.wiktor.ta.command.AttractionCommand
import pl.szymanski.wiktor.ta.command.BookAccommodationCommand
import pl.szymanski.wiktor.ta.command.BookAttractionCommand
import pl.szymanski.wiktor.ta.command.BookCommuteCommand
import pl.szymanski.wiktor.ta.command.CancelAccommodationBookingCommand
import pl.szymanski.wiktor.ta.command.CancelAttractionBookingCommand
import pl.szymanski.wiktor.ta.command.CancelCommuteBookingCommand
import pl.szymanski.wiktor.ta.command.CommuteCommand
import pl.szymanski.wiktor.ta.commandHandler.AccommodationCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.AttractionCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.CommuteCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.TravelOfferCommandHandler
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferEvent
import pl.szymanski.wiktor.ta.event.AccommodationBookedCompensatedEvent
import pl.szymanski.wiktor.ta.event.AccommodationBookingCanceledCompensatedEvent
import pl.szymanski.wiktor.ta.event.AttractionBookedCompensatedEvent
import pl.szymanski.wiktor.ta.event.AttractionBookingCanceledCompensatedEvent
import pl.szymanski.wiktor.ta.event.CommuteBookedCompensatedEvent
import pl.szymanski.wiktor.ta.event.CommuteBookingCanceledCompensatedEvent
import pl.szymanski.wiktor.ta.event.TravelOfferBookedCompensatedEvent
import pl.szymanski.wiktor.ta.event.TravelOfferBookingCanceledCompensatedEvent
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.fail

class BookingSagaTest {
    private val travelOfferCommandHandler = mockk<TravelOfferCommandHandler>(relaxed = true)
    private val attractionCommandHandler = mockk<AttractionCommandHandler>(relaxed = true)
    private val commuteCommandHandler = mockk<CommuteCommandHandler>(relaxed = true)
    private val accommodationCommandHandler = mockk<AccommodationCommandHandler>(relaxed = true)

    private lateinit var travelOfferId: UUID
    private lateinit var accommodationId: UUID
    private lateinit var commuteId: UUID
    private lateinit var attractionId: UUID
    private lateinit var userId: UUID
    private lateinit var correlationId: UUID
    private lateinit var seat: Seat

    private lateinit var triggeringBookedEvent: TravelOfferBookedEvent
    private lateinit var triggeringCanceledEvent: TravelOfferBookingCanceledEvent

    fun mockTravelOfferCommandHandler() {
        coEvery { travelOfferCommandHandler.compensate(any<TravelOfferBookedEvent>()) } answers {
            val event = firstArg<TravelOfferBookedEvent>()
            TravelOfferBookedCompensatedEvent(
                travelOfferId = event.travelOfferId,
                accommodationId = event.accommodationId,
                commuteId = event.commuteId,
                attractionId = event.attractionId,
                userId = event.userId,
                seat = event.seat,
                correlationId = event.correlationId,
            )
        }

        coEvery { travelOfferCommandHandler.compensate(any<TravelOfferBookingCanceledEvent>()) } answers {
            val event = firstArg<TravelOfferBookingCanceledEvent>()
            TravelOfferBookingCanceledCompensatedEvent(
                travelOfferId = event.travelOfferId,
                accommodationId = event.accommodationId,
                commuteId = event.commuteId,
                attractionId = event.attractionId,
                userId = event.userId,
                seat = event.seat,
                correlationId = event.correlationId,
            )
        }

        coEvery { travelOfferCommandHandler.compensate(any<TravelOfferEvent>()) } coAnswers {
            val event = firstArg<TravelOfferEvent>()
            when (event) {
                is TravelOfferBookedEvent -> travelOfferCommandHandler.compensate(event)
                is TravelOfferBookingCanceledEvent -> travelOfferCommandHandler.compensate(event)
                else -> fail("Unexpected event: $event")
            }
        }
    }

    fun mockAttractionCommandHandler() {
        coEvery { attractionCommandHandler.handle(any<BookAttractionCommand>()) } answers {
            val command = firstArg<BookAttractionCommand>()
            AttractionBookedEvent(
                attractionId = command.attractionId,
                userId = command.userId,
            )
        }

        coEvery { attractionCommandHandler.handle(any<CancelAttractionBookingCommand>()) } answers {
            val command = firstArg<CancelAttractionBookingCommand>()
            AttractionBookingCanceledEvent(
                attractionId = command.attractionId,
                userId = command.userId,
            )
        }

        coEvery { attractionCommandHandler.handle(any<AttractionCommand>()) } coAnswers {
            val command = firstArg<AttractionCommand>()
            when (command) {
                is BookAttractionCommand -> attractionCommandHandler.handle(command)
                is CancelAttractionBookingCommand -> attractionCommandHandler.handle(command)
                else -> fail("Unexpected command $command")
            }
        }

        coEvery { attractionCommandHandler.compensate(any<AttractionEvent>()) } coAnswers {
            val event = firstArg<AttractionEvent>()
            when (event) {
                is AttractionBookedEvent -> attractionCommandHandler.compensate(event)
                is AttractionBookingCanceledEvent -> attractionCommandHandler.compensate(event)
                else -> fail("Unexpected event $event")
            }
        }

        coEvery { attractionCommandHandler.compensate(any<AttractionBookedEvent>()) } answers {
            val event = firstArg<AttractionBookedEvent>()
            AttractionBookedCompensatedEvent(
                attractionId = event.attractionId,
                userId = event.userId,
                correlationId = event.correlationId,
            )
        }

        coEvery { attractionCommandHandler.compensate(any<AttractionBookingCanceledEvent>()) } answers {
            val event = firstArg<AttractionBookingCanceledEvent>()
            AttractionBookingCanceledCompensatedEvent(
                attractionId = event.attractionId,
                userId = event.userId,
                correlationId = event.correlationId,
            )
        }
    }

    fun mockCommuteCommandHandler() {
        coEvery { commuteCommandHandler.handle(any<BookCommuteCommand>()) } answers {
            val command = firstArg<BookCommuteCommand>()
            CommuteBookedEvent(
                commuteId = command.commuteId,
                userId = command.userId,
                seat = command.seat,
            )
        }

        coEvery { commuteCommandHandler.handle(any<CancelCommuteBookingCommand>()) } answers {
            val command = firstArg<CancelCommuteBookingCommand>()
            CommuteBookingCanceledEvent(
                commuteId = command.commuteId,
                userId = command.userId,
                seat = command.seat,
            )
        }

        coEvery { commuteCommandHandler.compensate(any<CommuteBookedEvent>()) } answers {
            val event = firstArg<CommuteBookedEvent>()
            CommuteBookedCompensatedEvent(
                commuteId = event.commuteId,
                userId = event.userId,
                seat = event.seat,
                correlationId = event.correlationId,
            )
        }

        coEvery { commuteCommandHandler.compensate(any<CommuteBookingCanceledEvent>()) } answers {
            val event = firstArg<CommuteBookingCanceledEvent>()
            CommuteBookingCanceledCompensatedEvent(
                commuteId = event.commuteId,
                userId = event.userId,
                seat = event.seat,
                correlationId = event.correlationId,
            )
        }

        coEvery { commuteCommandHandler.handle(any<CommuteCommand>()) } coAnswers {
            val command = firstArg<CommuteCommand>()
            when (command) {
                is BookCommuteCommand -> commuteCommandHandler.handle(command)
                is CancelCommuteBookingCommand -> commuteCommandHandler.handle(command)
                else -> fail("Unexpected command $command")
            }
        }

        coEvery { commuteCommandHandler.compensate(any<CommuteEvent>()) } coAnswers {
            val event = firstArg<CommuteEvent>()
            when (event) {
                is CommuteBookedEvent -> commuteCommandHandler.compensate(event)
                is CommuteBookingCanceledEvent -> commuteCommandHandler.compensate(event)
                else -> fail("Unexpected event $event")
            }
        }
    }

    fun mockAccommodationCommandHandler() {
        coEvery { accommodationCommandHandler.handle(any<BookAccommodationCommand>()) } answers {
            val command = firstArg<BookAccommodationCommand>()
            AccommodationBookedEvent(
                accommodationId = command.accommodationId,
                userId = command.userId,
            )
        }

        coEvery { accommodationCommandHandler.handle(any<CancelAccommodationBookingCommand>()) } answers {
            val command = firstArg<CancelAccommodationBookingCommand>()
            AccommodationBookingCanceledEvent(
                accommodationId = command.accommodationId,
                userId = command.userId,
            )
        }

        coEvery { accommodationCommandHandler.compensate(any<AccommodationBookedEvent>()) } answers {
            val event = firstArg<AccommodationBookedEvent>()
            AccommodationBookedCompensatedEvent(
                accommodationId = event.accommodationId,
                userId = event.userId,
                correlationId = event.correlationId,
            )
        }

        coEvery { accommodationCommandHandler.compensate(any<AccommodationBookingCanceledEvent>()) } answers {
            val event = firstArg<AccommodationBookingCanceledEvent>()
            AccommodationBookingCanceledCompensatedEvent(
                accommodationId = event.accommodationId,
                userId = event.userId,
                correlationId = event.correlationId,
            )
        }

        coEvery { accommodationCommandHandler.handle(any<AccommodationCommand>()) } coAnswers {
            val command = firstArg<AccommodationCommand>()
            when (command) {
                is BookAccommodationCommand -> accommodationCommandHandler.handle(command)
                is CancelAccommodationBookingCommand -> accommodationCommandHandler.handle(command)
                else -> fail("Unexpected command $command")
            }
        }

        coEvery { accommodationCommandHandler.compensate(any<AccommodationEvent>()) } coAnswers {
            val event = firstArg<AccommodationEvent>()
            when (event) {
                is AccommodationBookedEvent -> accommodationCommandHandler.compensate(event)
                is AccommodationBookingCanceledEvent -> accommodationCommandHandler.compensate(event)
                else -> fail("Unexpected event $event")
            }
        }
    }

    @BeforeTest
    fun setup() {
        travelOfferId = UUID.randomUUID()
        accommodationId = UUID.randomUUID()
        commuteId = UUID.randomUUID()
        attractionId = UUID.randomUUID()
        userId = UUID.randomUUID()
        correlationId = UUID.randomUUID()
        seat = Seat("1", "A")

        mockTravelOfferCommandHandler()
        mockAttractionCommandHandler()
        mockCommuteCommandHandler()
        mockAccommodationCommandHandler()

        triggeringBookedEvent =
            TravelOfferBookedEvent(
                travelOfferId = travelOfferId,
                accommodationId = accommodationId,
                commuteId = commuteId,
                attractionId = attractionId,
                userId = userId,
                seat = seat,
                correlationId = correlationId,
            )

        triggeringCanceledEvent =
            TravelOfferBookingCanceledEvent(
                travelOfferId = travelOfferId,
                accommodationId = accommodationId,
                commuteId = commuteId,
                attractionId = attractionId,
                userId = userId,
                seat = seat,
                correlationId = correlationId,
            )
    }

    @Test
    fun `execute should succeed when all commands succeed`() =
        runTest {
            val saga =
                BookingSaga(
                    travelOfferCommandHandler,
                    attractionCommandHandler,
                    commuteCommandHandler,
                    accommodationCommandHandler,
                    triggeringBookedEvent,
                )

            saga.execute()

            coVerify(exactly = 1) { commuteCommandHandler.handle(any<BookCommuteCommand>()) }
            coVerify(exactly = 1) { accommodationCommandHandler.handle(any<BookAccommodationCommand>()) }
            coVerify(exactly = 1) { attractionCommandHandler.handle(any<BookAttractionCommand>()) }

            coVerify(exactly = 0) { commuteCommandHandler.compensate(any<CommuteBookedEvent>()) }
            coVerify(exactly = 0) { accommodationCommandHandler.compensate(any<AccommodationBookedEvent>()) }
            coVerify(exactly = 0) { attractionCommandHandler.compensate(any<AttractionBookedEvent>()) }
            coVerify(exactly = 0) { travelOfferCommandHandler.compensate(any<TravelOfferBookedEvent>()) }
        }

    @Test
    fun `execute should compensate when commute command fails`() =
        runTest {
            coEvery { commuteCommandHandler.handle(any<BookCommuteCommand>()) } throws
                IllegalArgumentException("Commute cannot be booked")

            val saga =
                BookingSaga(
                    travelOfferCommandHandler,
                    attractionCommandHandler,
                    commuteCommandHandler,
                    accommodationCommandHandler,
                    triggeringBookedEvent,
                )

            saga.execute()

            coVerify(exactly = 1) { commuteCommandHandler.handle(any<BookCommuteCommand>()) }
            coVerify(exactly = 1) { accommodationCommandHandler.handle(any<BookAccommodationCommand>()) }
            coVerify(exactly = 1) { attractionCommandHandler.handle(any<BookAttractionCommand>()) }

            coVerify(exactly = 0) { commuteCommandHandler.compensate(any<CommuteBookedEvent>()) }
            coVerify(exactly = 1) { accommodationCommandHandler.compensate(any<AccommodationBookedEvent>()) }
            coVerify(exactly = 1) { attractionCommandHandler.compensate(any<AttractionBookedEvent>()) }
            coVerify(exactly = 1) { travelOfferCommandHandler.compensate(any<TravelOfferBookedEvent>()) }
        }

    @Test
    fun `execute should compensate when accommodation command fails`() =
        runTest {
            coEvery { accommodationCommandHandler.handle(any<BookAccommodationCommand>()) } throws
                IllegalArgumentException("Accommodation cannot be booked")

            val saga =
                BookingSaga(
                    travelOfferCommandHandler,
                    attractionCommandHandler,
                    commuteCommandHandler,
                    accommodationCommandHandler,
                    triggeringBookedEvent,
                )

            saga.execute()

            coVerify(exactly = 1) { commuteCommandHandler.handle(any<BookCommuteCommand>()) }
            coVerify(exactly = 1) { accommodationCommandHandler.handle(any<BookAccommodationCommand>()) }
            coVerify(exactly = 1) { attractionCommandHandler.handle(any<BookAttractionCommand>()) }

            coVerify(exactly = 1) { commuteCommandHandler.compensate(any<CommuteBookedEvent>()) }
            coVerify(exactly = 0) { accommodationCommandHandler.compensate(any<AccommodationBookedEvent>()) }
            coVerify(exactly = 1) { attractionCommandHandler.compensate(any<AttractionBookedEvent>()) }
            coVerify(exactly = 1) { travelOfferCommandHandler.compensate(any<TravelOfferBookedEvent>()) }
        }

    @Test
    fun `execute should compensate when attraction command fails`() =
        runTest {
            coEvery { attractionCommandHandler.handle(any<BookAttractionCommand>()) } throws
                IllegalArgumentException("Attraction cannot be booked")

            val saga =
                BookingSaga(
                    travelOfferCommandHandler,
                    attractionCommandHandler,
                    commuteCommandHandler,
                    accommodationCommandHandler,
                    triggeringBookedEvent,
                )

            saga.execute()

            coVerify(exactly = 1) { commuteCommandHandler.handle(any<BookCommuteCommand>()) }
            coVerify(exactly = 1) { accommodationCommandHandler.handle(any<BookAccommodationCommand>()) }
            coVerify(exactly = 1) { attractionCommandHandler.handle(any<BookAttractionCommand>()) }

            coVerify(exactly = 1) { commuteCommandHandler.compensate(any<CommuteBookedEvent>()) }
            coVerify(exactly = 1) { accommodationCommandHandler.compensate(any<AccommodationBookedEvent>()) }
            coVerify(exactly = 0) { attractionCommandHandler.compensate(any<AttractionBookedEvent>()) }
            coVerify(exactly = 1) { travelOfferCommandHandler.compensate(any<TravelOfferBookedEvent>()) }
        }

    @Test
    fun `execute should not call attraction command handler when attractionId is null`() =
        runTest {
            val saga =
                BookingSaga(
                    travelOfferCommandHandler,
                    attractionCommandHandler,
                    commuteCommandHandler,
                    accommodationCommandHandler,
                    triggeringBookedEvent.copy(attractionId = null),
                )

            saga.execute()

            coVerify(exactly = 1) { commuteCommandHandler.handle(any<BookCommuteCommand>()) }
            coVerify(exactly = 1) { accommodationCommandHandler.handle(any<BookAccommodationCommand>()) }
            coVerify(exactly = 0) { attractionCommandHandler.handle(any<BookAttractionCommand>()) }

            coVerify(exactly = 0) { commuteCommandHandler.compensate(any<CommuteBookedEvent>()) }
            coVerify(exactly = 0) { accommodationCommandHandler.compensate(any<AccommodationBookedEvent>()) }
            coVerify(exactly = 0) { attractionCommandHandler.compensate(any<AttractionBookedEvent>()) }
            coVerify(exactly = 0) { travelOfferCommandHandler.compensate(any<TravelOfferBookedEvent>()) }
        }

    @Test
    fun `execute should compensate when one command fails and attractionId is null`() =
        runTest {
            coEvery { commuteCommandHandler.handle(any<BookCommuteCommand>()) } throws
                IllegalArgumentException("Commute cannot be booked")

            val saga =
                BookingSaga(
                    travelOfferCommandHandler,
                    attractionCommandHandler,
                    commuteCommandHandler,
                    accommodationCommandHandler,
                    triggeringBookedEvent.copy(attractionId = null),
                )

            saga.execute()

            coVerify(exactly = 1) { commuteCommandHandler.handle(any<BookCommuteCommand>()) }
            coVerify(exactly = 1) { accommodationCommandHandler.handle(any<BookAccommodationCommand>()) }
            coVerify(exactly = 0) { attractionCommandHandler.handle(any<BookAttractionCommand>()) }

            coVerify(exactly = 0) { commuteCommandHandler.compensate(any<CommuteBookedEvent>()) }
            coVerify(exactly = 1) { accommodationCommandHandler.compensate(any<AccommodationBookedEvent>()) }
            coVerify(exactly = 0) { attractionCommandHandler.compensate(any<AttractionBookedEvent>()) }
            coVerify(exactly = 1) { travelOfferCommandHandler.compensate(any<TravelOfferBookedEvent>()) }
        }

    @Test
    fun `execute of cancel event should succeed when all commands succeed`() =
        runTest {
            val saga =
                BookingSaga(
                    travelOfferCommandHandler,
                    attractionCommandHandler,
                    commuteCommandHandler,
                    accommodationCommandHandler,
                    triggeringCanceledEvent,
                )

            saga.execute()

            coVerify(exactly = 1) { commuteCommandHandler.handle(any<CancelCommuteBookingCommand>()) }
            coVerify(exactly = 1) { accommodationCommandHandler.handle(any<CancelAccommodationBookingCommand>()) }
            coVerify(exactly = 1) { attractionCommandHandler.handle(any<CancelAttractionBookingCommand>()) }

            coVerify(exactly = 0) { commuteCommandHandler.compensate(any<CommuteBookingCanceledEvent>()) }
            coVerify(exactly = 0) { accommodationCommandHandler.compensate(any<AccommodationBookingCanceledEvent>()) }
            coVerify(exactly = 0) { attractionCommandHandler.compensate(any<AttractionBookingCanceledEvent>()) }
            coVerify(exactly = 0) { travelOfferCommandHandler.compensate(any<TravelOfferBookingCanceledEvent>()) }
        }

    @Test
    fun `execute of cancel event should compensate when commute command fails`() =
        runTest {
            coEvery { commuteCommandHandler.handle(any<CancelCommuteBookingCommand>()) } throws
                IllegalArgumentException("Commute booking cannot be canceled")

            val saga =
                BookingSaga(
                    travelOfferCommandHandler,
                    attractionCommandHandler,
                    commuteCommandHandler,
                    accommodationCommandHandler,
                    triggeringCanceledEvent,
                )

            saga.execute()

            coVerify(exactly = 1) { commuteCommandHandler.handle(any<CancelCommuteBookingCommand>()) }
            coVerify(exactly = 1) { accommodationCommandHandler.handle(any<CancelAccommodationBookingCommand>()) }
            coVerify(exactly = 1) { attractionCommandHandler.handle(any<CancelAttractionBookingCommand>()) }

            coVerify(exactly = 0) { commuteCommandHandler.compensate(any<CommuteBookingCanceledEvent>()) }
            coVerify(exactly = 1) { accommodationCommandHandler.compensate(any<AccommodationBookingCanceledEvent>()) }
            coVerify(exactly = 1) { attractionCommandHandler.compensate(any<AttractionBookingCanceledEvent>()) }
            coVerify(exactly = 1) { travelOfferCommandHandler.compensate(any<TravelOfferBookingCanceledEvent>()) }
        }

    @Test
    fun `execute of cancel event should compensate when accommodation command fails`() =
        runTest {
            coEvery { accommodationCommandHandler.handle(any<CancelAccommodationBookingCommand>()) } throws
                IllegalArgumentException("Accommodation booking cannot be canceled")

            val saga =
                BookingSaga(
                    travelOfferCommandHandler,
                    attractionCommandHandler,
                    commuteCommandHandler,
                    accommodationCommandHandler,
                    triggeringCanceledEvent,
                )

            saga.execute()

            coVerify(exactly = 1) { commuteCommandHandler.handle(any<CancelCommuteBookingCommand>()) }
            coVerify(exactly = 1) { accommodationCommandHandler.handle(any<CancelAccommodationBookingCommand>()) }
            coVerify(exactly = 1) { attractionCommandHandler.handle(any<CancelAttractionBookingCommand>()) }

            coVerify(exactly = 1) { commuteCommandHandler.compensate(any<CommuteBookingCanceledEvent>()) }
            coVerify(exactly = 0) { accommodationCommandHandler.compensate(any<AccommodationBookingCanceledEvent>()) }
            coVerify(exactly = 1) { attractionCommandHandler.compensate(any<AttractionBookingCanceledEvent>()) }
            coVerify(exactly = 1) { travelOfferCommandHandler.compensate(any<TravelOfferBookingCanceledEvent>()) }
        }

    @Test
    fun `execute of cancel event should compensate when attraction command fails`() =
        runTest {
            coEvery { attractionCommandHandler.handle(any<CancelAttractionBookingCommand>()) } throws
                IllegalArgumentException("Attraction booking cannot be canceled")

            val saga =
                BookingSaga(
                    travelOfferCommandHandler,
                    attractionCommandHandler,
                    commuteCommandHandler,
                    accommodationCommandHandler,
                    triggeringCanceledEvent,
                )

            saga.execute()

            coVerify(exactly = 1) { commuteCommandHandler.handle(any<CancelCommuteBookingCommand>()) }
            coVerify(exactly = 1) { accommodationCommandHandler.handle(any<CancelAccommodationBookingCommand>()) }
            coVerify(exactly = 1) { attractionCommandHandler.handle(any<CancelAttractionBookingCommand>()) }

            coVerify(exactly = 1) { commuteCommandHandler.compensate(any<CommuteBookingCanceledEvent>()) }
            coVerify(exactly = 1) { accommodationCommandHandler.compensate(any<AccommodationBookingCanceledEvent>()) }
            coVerify(exactly = 0) { attractionCommandHandler.compensate(any<AttractionBookingCanceledEvent>()) }
            coVerify(exactly = 1) { travelOfferCommandHandler.compensate(any<TravelOfferBookingCanceledEvent>()) }
        }

    @Test
    fun `execute of cancel event should not call attraction command handler when attractionId is null`() =
        runTest {
            val saga =
                BookingSaga(
                    travelOfferCommandHandler,
                    attractionCommandHandler,
                    commuteCommandHandler,
                    accommodationCommandHandler,
                    triggeringCanceledEvent.copy(attractionId = null),
                )

            saga.execute()

            coVerify(exactly = 1) { commuteCommandHandler.handle(any<CancelCommuteBookingCommand>()) }
            coVerify(exactly = 1) { accommodationCommandHandler.handle(any<CancelAccommodationBookingCommand>()) }
            coVerify(exactly = 0) { attractionCommandHandler.handle(any<CancelAttractionBookingCommand>()) }

            coVerify(exactly = 0) { commuteCommandHandler.compensate(any<CommuteBookingCanceledEvent>()) }
            coVerify(exactly = 0) { accommodationCommandHandler.compensate(any<AccommodationBookingCanceledEvent>()) }
            coVerify(exactly = 0) { attractionCommandHandler.compensate(any<AttractionBookingCanceledEvent>()) }
            coVerify(exactly = 0) { travelOfferCommandHandler.compensate(any<TravelOfferBookingCanceledEvent>()) }
        }

    @Test
    fun `execute of cancel event should compensate when one command fails and attractionId is null`() =
        runTest {
            coEvery { commuteCommandHandler.handle(any<CancelCommuteBookingCommand>()) } throws
                IllegalArgumentException("Commute booking cannot be canceled")

            val saga =
                BookingSaga(
                    travelOfferCommandHandler,
                    attractionCommandHandler,
                    commuteCommandHandler,
                    accommodationCommandHandler,
                    triggeringCanceledEvent.copy(attractionId = null),
                )

            saga.execute()

            coVerify(exactly = 1) { commuteCommandHandler.handle(any<CancelCommuteBookingCommand>()) }
            coVerify(exactly = 1) { accommodationCommandHandler.handle(any<CancelAccommodationBookingCommand>()) }
            coVerify(exactly = 0) { attractionCommandHandler.handle(any<CancelAttractionBookingCommand>()) }

            coVerify(exactly = 0) { commuteCommandHandler.compensate(any<CommuteBookingCanceledEvent>()) }
            coVerify(exactly = 1) { accommodationCommandHandler.compensate(any<AccommodationBookingCanceledEvent>()) }
            coVerify(exactly = 0) { attractionCommandHandler.compensate(any<AttractionBookingCanceledEvent>()) }
            coVerify(exactly = 1) { travelOfferCommandHandler.compensate(any<TravelOfferBookingCanceledEvent>()) }
        }
}
