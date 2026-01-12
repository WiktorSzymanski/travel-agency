package pl.szymanski.wiktor.ta.saga

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import pl.szymanski.wiktor.ta.DummyCommandBus
import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.command.*
import pl.szymanski.wiktor.ta.commandhandler.AccommodationCommandHandler
import pl.szymanski.wiktor.ta.commandhandler.AttractionCommandHandler
import pl.szymanski.wiktor.ta.commandhandler.BookingCommandHandler
import pl.szymanski.wiktor.ta.commandhandler.CommuteCommandHandler
import pl.szymanski.wiktor.ta.domain.BookingState
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.aggregate.*
import pl.szymanski.wiktor.ta.domain.event.*
import pl.szymanski.wiktor.ta.domain.exception.AccommodationException
import pl.szymanski.wiktor.ta.domain.exception.AttractionException
import pl.szymanski.wiktor.ta.domain.exception.CommuteException
import pl.szymanski.wiktor.ta.event.BookingSagaCompletedEvent
import pl.szymanski.wiktor.ta.event.BookingSagaFailedEvent
import pl.szymanski.wiktor.ta.event.BookingSagaStartedEvent
import java.util.*
import kotlin.test.BeforeTest
import kotlin.test.Test

class BookingSagaNewTest {
    private lateinit var commandBus: DummyCommandBus

    val bookingCommandHandler = mockk<BookingCommandHandler>(relaxed = true)
    val commuteCommandHandler = mockk<CommuteCommandHandler>(relaxed = true)
    val attractionCommandHandler = mockk<AttractionCommandHandler>(relaxed = true)
    val accommodationCommandHandler = mockk<AccommodationCommandHandler>(relaxed = true)

    val commuteAgg = mockk<Commute>(relaxed = true)
    val accommodationAgg = mockk<Accommodation>(relaxed = true)
    val attractionAgg = mockk<Attraction>(relaxed = true)

    val eventBus = mockk<EventBus>(relaxed = true)

    val sagaRepository = mockk<SagaRepository>(relaxed = true)

    private val metadata = Metadata(UUID.randomUUID(), 1)

    @BeforeTest
    fun setup() {
        commandBus = DummyCommandBus(
            bookingCommandHandler,
            commuteCommandHandler,
            attractionCommandHandler,
            accommodationCommandHandler,
        )

        coEvery { commuteCommandHandler.handle(match { it is BookCommuteCommand }) } answers {
            val command = firstArg<BookCommuteCommand>()
            commuteAgg to listOf(
                CommuteBookedEvent(
                    commuteId = command.commuteId,
                    bookingId = command.bookingId,
                    seat = command.seat
                )
            )
        }

        coEvery { accommodationCommandHandler.handle(match { it is BookAccommodationCommand }) } answers {
            val command = firstArg<BookAccommodationCommand>()
            accommodationAgg to listOf(
                AccommodationBookedEvent(
                    accommodationId = command.accommodationId,
                    bookingId = command.bookingId,
                ),
            )
        }

        coEvery { attractionCommandHandler.handle(match { it is BookAttractionCommand }) } answers {
            val command = firstArg<BookAttractionCommand>()
            attractionAgg to listOf(
                AttractionBookedEvent(
                    attractionId = command.attractionId,
                    bookingId = command.bookingId,
                ),
            )
        }

        coEvery { commuteCommandHandler.handle(match { it is CompensateBookCommuteCommand }) } answers {
            val command = firstArg<CompensateBookCommuteCommand>()
            commuteAgg to listOf(
                CommuteBookedCompensatedEvent(
                    commuteId = command.commuteId,
                    bookingId = command.bookingId,
                    seat = Seat.Any
                )
            )
        }

        coEvery { accommodationCommandHandler.handle(match { it is CompensateBookAccommodationCommand }) } answers {
            val command = firstArg<CompensateBookAccommodationCommand>()
            accommodationAgg to listOf(
                AccommodationBookedCompensatedEvent(
                    accommodationId = command.accommodationId,
                    bookingId = command.bookingId,
                ),
            )
        }
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

    @Test
    fun `saga success with attraction should complete and publish events`() =
        runTest {
            // Given
            val triggering = reservedEvent(attractionId = AttractionId.generate())

            val sagaState = SagaState(
                id = UUID.randomUUID(),
                type = SagaType.BOOKING,
                travelOffer = triggering.travelOffer,
                bookingId = triggering.bookingId,
                seat = triggering.seat,
            )

            val saga = PersistentBookingSaga(
                eventBus,
                commandBus,
                sagaRepository,
                sagaState,
                metadata)

            // When
            saga.executeOrResume()

            // Then
            coVerify(exactly = 1) { commuteCommandHandler.handle(match { it is BookCommuteCommand }) }
            coVerify(exactly = 1) { accommodationCommandHandler.handle(match { it is BookAccommodationCommand }) }
            coVerify(exactly = 1) { attractionCommandHandler.handle(match { it is BookAttractionCommand }) }

            coVerify(exactly = 0) { commuteCommandHandler.handle(match { it is CompensateBookCommuteCommand }) }
            coVerify(exactly = 0) { accommodationCommandHandler.handle(match { it is CompensateBookAccommodationCommand }) }

            coVerify(exactly = 1) { eventBus.publish(match { it.event is BookingSagaStartedEvent })}
            coVerify(exactly = 1) { eventBus.publish(match { it.event is BookingSagaCompletedEvent })}
            coVerify(exactly = 0) { eventBus.publish(match { it.event is BookingSagaFailedEvent })}
        }

    @Test
    fun `saga success without attraction should complete and not call attraction handler`() =
        runTest {
            // Given
            val triggering = reservedEvent(attractionId = AttractionId.Empty)

            val sagaState = SagaState(
                id = UUID.randomUUID(),
                type = SagaType.BOOKING,
                travelOffer = triggering.travelOffer,
                bookingId = triggering.bookingId,
                seat = triggering.seat,
            )

            val saga = PersistentBookingSaga(
                eventBus,
                commandBus,
                sagaRepository,
                sagaState,
                metadata)

            // When
            saga.executeOrResume()


            // Then
            coVerify(exactly = 1) { commuteCommandHandler.handle(match { it is BookCommuteCommand }) }
            coVerify(exactly = 1) { accommodationCommandHandler.handle(match { it is BookAccommodationCommand }) }
            coVerify(exactly = 0) { attractionCommandHandler.handle(match { it is BookAttractionCommand }) }

            coVerify(exactly = 0) { commuteCommandHandler.handle(match { it is CompensateBookCommuteCommand }) }
            coVerify(exactly = 0) { accommodationCommandHandler.handle(match { it is CompensateBookAccommodationCommand }) }

            coVerify(exactly = 1) { eventBus.publish(match { it.event is BookingSagaStartedEvent })}
            coVerify(exactly = 1) { eventBus.publish(match { it.event is BookingSagaCompletedEvent })}
            coVerify(exactly = 0) { eventBus.publish(match { it.event is BookingSagaFailedEvent })}
        }

    @Test
    fun `saga commute failure should publish failed and stop`() =
        runTest {
            // Given
            val triggering = reservedEvent()

            val sagaState = SagaState(
                id = UUID.randomUUID(),
                type = SagaType.BOOKING,
                travelOffer = triggering.travelOffer,
                bookingId = triggering.bookingId,
                seat = triggering.seat,
            )

            val saga = PersistentBookingSaga(
                eventBus,
                commandBus,
                sagaRepository,
                sagaState,
                metadata)

            coEvery {
                commuteCommandHandler.handle(match { it is BookCommuteCommand })
            } throws CommuteException("Commute booking failed")

            // When
            saga.executeOrResume()


            // Then
            coVerify(exactly = 1) { commuteCommandHandler.handle(match { it is BookCommuteCommand }) }
            coVerify(exactly = 0) { accommodationCommandHandler.handle(match { it is BookAccommodationCommand }) }
            coVerify(exactly = 0) { attractionCommandHandler.handle(match { it is BookAttractionCommand }) }

            coVerify(exactly = 0) { commuteCommandHandler.handle(match { it is CompensateBookCommuteCommand }) }
            coVerify(exactly = 0) { accommodationCommandHandler.handle(match { it is CompensateBookAccommodationCommand }) }

            coVerify(exactly = 1) { eventBus.publish(match { it.event is BookingSagaStartedEvent })}
            coVerify(exactly = 0) { eventBus.publish(match { it.event is BookingSagaCompletedEvent })}
            coVerify(exactly = 1) { eventBus.publish(match { it.event is BookingSagaFailedEvent })}
        }

    @Test
    fun `saga accommodation failure should compensate commute and publish failed`() =
        runTest {
            // Given
            val triggering = reservedEvent(attractionId = AttractionId.Empty)

            coEvery {
                accommodationCommandHandler.handle(match { it is BookAccommodationCommand })
            } throws AccommodationException("Accommodation booking failed")

            val sagaState = SagaState(
                id = UUID.randomUUID(),
                type = SagaType.BOOKING,
                travelOffer = triggering.travelOffer,
                bookingId = triggering.bookingId,
                seat = triggering.seat,
            )

            val saga = PersistentBookingSaga(
                eventBus,
                commandBus,
                sagaRepository,
                sagaState,
                metadata)

            // When
            saga.executeOrResume()


            // Then
            coVerify(exactly = 1) { commuteCommandHandler.handle(match { it is BookCommuteCommand }) }
            coVerify(exactly = 1) { accommodationCommandHandler.handle(match { it is BookAccommodationCommand }) }
            coVerify(exactly = 0) { attractionCommandHandler.handle(match { it is BookAttractionCommand }) }

            coVerify(exactly = 1) { commuteCommandHandler.handle(match { it is CompensateBookCommuteCommand }) }
            coVerify(exactly = 0) { accommodationCommandHandler.handle(match { it is CompensateBookAccommodationCommand }) }

            coVerify(exactly = 1) { eventBus.publish(match { it.event is BookingSagaStartedEvent })}
            coVerify(exactly = 0) { eventBus.publish(match { it.event is BookingSagaCompletedEvent })}
            coVerify(exactly = 1) { eventBus.publish(match { it.event is BookingSagaFailedEvent })}
        }

    @Test
    fun `saga attraction failure should compensate accommodation and commute and publish failed`() =
        runTest {
            // Given
            val triggering = reservedEvent(attractionId = AttractionId.generate())

            coEvery {
                attractionCommandHandler.handle(match { it is BookAttractionCommand })
            } throws AttractionException("Attraction booking failed")

            val sagaState = SagaState(
                type = SagaType.BOOKING,
                travelOffer = triggering.travelOffer,
                bookingId = triggering.bookingId,
                seat = triggering.seat,
            )

            val saga = PersistentBookingSaga(
                eventBus,
                commandBus,
                sagaRepository,
                sagaState,
                metadata)

            // When
            saga.executeOrResume()


            // Then
            coVerify(exactly = 1) { commuteCommandHandler.handle(match { it is BookCommuteCommand }) }
            coVerify(exactly = 1) { accommodationCommandHandler.handle(match { it is BookAccommodationCommand }) }
            coVerify(exactly = 1) { attractionCommandHandler.handle(match { it is BookAttractionCommand }) }

            coVerify(exactly = 1) { commuteCommandHandler.handle(match { it is CompensateBookCommuteCommand }) }
            coVerify(exactly = 1) { accommodationCommandHandler.handle(match { it is CompensateBookAccommodationCommand }) }

            coVerify(exactly = 1) { eventBus.publish(match { it.event is BookingSagaStartedEvent })}
            coVerify(exactly = 0) { eventBus.publish(match { it.event is BookingSagaCompletedEvent })}
            coVerify(exactly = 1) { eventBus.publish(match { it.event is BookingSagaFailedEvent })}
        }

//    val l = listOf(
//        SagaState(
//            type = SagaType.BOOKING,
//            travelOffer = triggering.travelOffer,
//            bookingId = triggering.bookingId,
//            seat = triggering.seat,
//            status = SagaStatus.NEW
//        ),
//        SagaState(
//            type = SagaType.BOOKING,
//            travelOffer = triggering.travelOffer,
//            bookingId = triggering.bookingId,
//            seat = triggering.seat,
//            status = SagaStatus.PROCESSING,
//            step = SagaStep.PENDING_COMMUTE
//        ),
//        SagaState(
//            type = SagaType.BOOKING,
//            travelOffer = triggering.travelOffer,
//            bookingId = triggering.bookingId,
//            seat = triggering.seat,
//            status = SagaStatus.PROCESSING,
//            step = SagaStep.PENDING_ACCOMMODATION
//        ),
//        SagaState(
//            type = SagaType.BOOKING,
//            travelOffer = triggering.travelOffer,
//            bookingId = triggering.bookingId,
//            seat = triggering.seat,
//            status = SagaStatus.PROCESSING,
//            step = SagaStep.PENDING_ATTRACTION
//        ),
//        SagaState(
//            type = SagaType.BOOKING,
//            travelOffer = triggering.travelOffer,
//            bookingId = triggering.bookingId,
//            seat = triggering.seat,
//            status = SagaStatus.COMPLETED
//        ),
//        SagaState(
//            type = SagaType.BOOKING,
//            travelOffer = triggering.travelOffer,
//            bookingId = triggering.bookingId,
//            seat = triggering.seat,
//            status = SagaStatus.COMPENSATING,
//            step = SagaStep.COMPENSATING_ACCOMMODATION
//        ),
//        SagaState(
//            type = SagaType.BOOKING,
//            travelOffer = triggering.travelOffer,
//            bookingId = triggering.bookingId,
//            seat = triggering.seat,
//            status = SagaStatus.COMPENSATING,
//            step = SagaStep.COMPENSATING_COMMUTE
//        ),
//        SagaState(
//            type = SagaType.BOOKING,
//            travelOffer = triggering.travelOffer,
//            bookingId = triggering.bookingId,
//            seat = triggering.seat,
//            status = SagaStatus.FAILED
//        ),
//    )
}
