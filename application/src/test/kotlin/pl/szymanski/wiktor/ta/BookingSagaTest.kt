package pl.szymanski.wiktor.ta

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import pl.szymanski.wiktor.ta.command.accommodation.AccommodationCommand
import pl.szymanski.wiktor.ta.command.accommodation.AccommodationCommandHandler
import pl.szymanski.wiktor.ta.command.accommodation.BookAccommodationCommand
import pl.szymanski.wiktor.ta.command.attraction.AttractionCommand
import pl.szymanski.wiktor.ta.command.attraction.AttractionCommandHandler
import pl.szymanski.wiktor.ta.command.attraction.BookAttractionCommand
import pl.szymanski.wiktor.ta.command.commute.BookCommuteCommand
import pl.szymanski.wiktor.ta.command.commute.CommuteCommand
import pl.szymanski.wiktor.ta.command.commute.CommuteCommandHandler
import pl.szymanski.wiktor.ta.command.travelOffer.BookTravelOfferCommand
import pl.szymanski.wiktor.ta.command.travelOffer.TravelOfferCommandHandler
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookedEvent
import pl.szymanski.wiktor.ta.event.AccommodationBookedCompensatedEvent
import pl.szymanski.wiktor.ta.event.AttractionBookedCompensatedEvent
import pl.szymanski.wiktor.ta.event.CommuteBookedCompensatedEvent
import pl.szymanski.wiktor.ta.event.TravelOfferBookedCompensatedEvent
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test

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

    private lateinit var saga: BookingSaga

    private lateinit var triggeringEvent: TravelOfferBookedEvent

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
    }

    fun mockAttractionCommandHandler() {
        coEvery { attractionCommandHandler.handle(any<BookAttractionCommand>()) } answers {
            val command = firstArg<BookAttractionCommand>()
            AttractionBookedEvent(
                attractionId = command.attractionId,
                userId = command.userId,
            )
        }

        coEvery { attractionCommandHandler.compensate(any<AttractionBookedEvent>()) } answers {
            val event = firstArg<AttractionBookedEvent>()
            AttractionBookedCompensatedEvent(
                attractionId = event.attractionId,
                userId = event.userId,
                correlationId = event.correlationId,
            )
        }

        coEvery { attractionCommandHandler.handle(any<AttractionCommand>()) } coAnswers {
            val command = firstArg<AttractionCommand>()
            when (command) {
                is BookAttractionCommand -> attractionCommandHandler.handle(command as BookAttractionCommand)
                else -> throw IllegalArgumentException("Unknown command")
            }
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

        coEvery { commuteCommandHandler.compensate(any<CommuteBookedEvent>()) } answers {
            val event = firstArg<CommuteBookedEvent>()
            CommuteBookedCompensatedEvent(
                commuteId = event.commuteId,
                userId = event.userId,
                seat = event.seat,
                correlationId = event.correlationId,
            )
        }

        coEvery { commuteCommandHandler.handle(any<CommuteCommand>()) } coAnswers {
            val command = firstArg<CommuteCommand>()
            when (command) {
                is BookCommuteCommand -> commuteCommandHandler.handle(command as BookCommuteCommand)
                else -> throw IllegalArgumentException("Unknown command")
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

        coEvery { accommodationCommandHandler.compensate(any<AccommodationBookedEvent>()) } answers {
            val event = firstArg<AccommodationBookedEvent>()
            AccommodationBookedCompensatedEvent(
                accommodationId = event.accommodationId,
                userId = event.userId,
                correlationId = event.correlationId,
            )
        }

        coEvery { accommodationCommandHandler.handle(any<AccommodationCommand>()) } coAnswers {
            val command = firstArg<AccommodationCommand>()
            when (command) {
                is BookAccommodationCommand -> accommodationCommandHandler.handle(command as BookAccommodationCommand)
                else -> throw IllegalArgumentException("Unknown command")
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

        triggeringEvent =
            TravelOfferBookedEvent(
                travelOfferId = travelOfferId,
                accommodationId = accommodationId,
                commuteId = commuteId,
                attractionId = attractionId,
                userId = userId,
                seat = seat,
                correlationId = correlationId,
            )

        saga =
            BookingSaga(
                travelOfferCommandHandler,
                attractionCommandHandler,
                commuteCommandHandler,
                accommodationCommandHandler,
                triggeringEvent,
            )
    }

    @Test
    fun `execute should succeed when all commands succeed`() =
        runTest {
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
                    triggeringEvent.copy(attractionId = null),
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
                    triggeringEvent.copy(attractionId = null),
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
}
