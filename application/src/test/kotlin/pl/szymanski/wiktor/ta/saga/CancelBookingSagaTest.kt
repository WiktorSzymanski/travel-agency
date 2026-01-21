package pl.szymanski.wiktor.ta.saga

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import pl.szymanski.wiktor.ta.DummyCommandBus
import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.command.*
import pl.szymanski.wiktor.ta.commandhandler.AccommodationCommandHandler
import pl.szymanski.wiktor.ta.commandhandler.AttractionCommandHandler
import pl.szymanski.wiktor.ta.commandhandler.BookingCommandHandler
import pl.szymanski.wiktor.ta.commandhandler.CommuteCommandHandler
import pl.szymanski.wiktor.ta.dlq.DeadLetterQueueRepository
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.aggregate.*
import pl.szymanski.wiktor.ta.domain.event.*
import pl.szymanski.wiktor.ta.domain.exception.AccommodationException
import pl.szymanski.wiktor.ta.domain.exception.AttractionException
import pl.szymanski.wiktor.ta.domain.exception.CommuteException
import pl.szymanski.wiktor.ta.event.BookingCancelSagaCompletedEvent
import pl.szymanski.wiktor.ta.event.BookingCancelSagaFailedEvent
import pl.szymanski.wiktor.ta.event.BookingCancelSagaStartedEvent
import pl.szymanski.wiktor.ta.outbox.SagaOutboxPort
import java.util.*
import kotlin.test.BeforeTest
import kotlin.test.Test

class CancelBookingSagaTest {
    private lateinit var commandBus: DummyCommandBus

    val bookingCommandHandler = mockk<BookingCommandHandler>(relaxed = true)
    val commuteCommandHandler = mockk<CommuteCommandHandler>(relaxed = true)
    val attractionCommandHandler = mockk<AttractionCommandHandler>(relaxed = true)
    val accommodationCommandHandler = mockk<AccommodationCommandHandler>(relaxed = true)

    val commuteAgg = mockk<Commute>(relaxed = true)
    val accommodationAgg = mockk<Accommodation>(relaxed = true)
    val attractionAgg = mockk<Attraction>(relaxed = true)

    val sagaRepository = mockk<SagaRepository>(relaxed = true)
    val sagaOutboxPort = mockk<SagaOutboxPort>(relaxed = true)
    val dlqRepository = mockk<DeadLetterQueueRepository>(relaxed = true)

    private val metadata = Metadata(UUID.randomUUID(), 1)

    @BeforeTest
    fun setup() {
        commandBus = DummyCommandBus(
            bookingCommandHandler,
            commuteCommandHandler,
            attractionCommandHandler,
            accommodationCommandHandler,
        )

        coEvery { commuteCommandHandler.handle(match { it is CancelCommuteBookingCommand }) } answers {
            val command = firstArg<CancelCommuteBookingCommand>()
            commuteAgg to listOf(
                CommuteBookingCanceledEvent(
                    commuteId = command.commuteId,
                    bookingId = command.bookingId,
                    seat = Seat.Any
                )
            )
        }

        coEvery { accommodationCommandHandler.handle(match { it is CancelAccommodationBookingCommand }) } answers {
            val command = firstArg<CancelAccommodationBookingCommand>()
            accommodationAgg to listOf(
                AccommodationBookingCanceledEvent(
                    accommodationId = command.accommodationId,
                    bookingId = command.bookingId,
                ),
            )
        }

        coEvery { attractionCommandHandler.handle(match { it is CancelAttractionBookingCommand }) } answers {
            val command = firstArg<CancelAttractionBookingCommand>()
            attractionAgg to listOf(
                AttractionBookingCanceledEvent(
                    attractionId = command.attractionId,
                    bookingId = command.bookingId,
                ),
            )
        }

        coEvery { commuteCommandHandler.handle(match { it is CompensateCancelCommuteBookingCommand }) } answers {
            val command = firstArg<CompensateCancelCommuteBookingCommand>()
            commuteAgg to listOf(
                CommuteBookingCanceledCompensatedEvent(
                    commuteId = command.commuteId,
                    bookingId = command.bookingId,
                    seat = Seat.Any
                )
            )
        }

        coEvery { accommodationCommandHandler.handle(match { it is CompensateCancelAccommodationBookingCommand }) } answers {
            val command = firstArg<CompensateCancelAccommodationBookingCommand>()
            accommodationAgg to listOf(
                AccommodationBookingCanceledCompensatedEvent(
                    accommodationId = command.accommodationId,
                    bookingId = command.bookingId,
                ),
            )
        }
    }

    private fun bookingCancelRequestedEvent(
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

    @Test
    fun `saga success with attraction should complete and publish events`() =
        runTest {
            // Given
            val triggering = bookingCancelRequestedEvent(attractionId = AttractionId.generate())

            val sagaState = SagaState(
                id = UUID.randomUUID(),
                type = SagaType.CANCELLING,
                travelOffer = triggering.travelOffer,
                bookingId = triggering.bookingId,
                seat = triggering.seat,
            )

            val saga = CancelBookingSaga(
                commandBus,
                sagaRepository,
                sagaOutboxPort,
                dlqRepository,
                sagaState,
                metadata)

            // When
            saga.executeOrResume()

            // Then
            coVerify(exactly = 1) { commuteCommandHandler.handle(match { it is CancelCommuteBookingCommand }) }
            coVerify(exactly = 1) { accommodationCommandHandler.handle(match { it is CancelAccommodationBookingCommand }) }
            coVerify(exactly = 1) { attractionCommandHandler.handle(match { it is CancelAttractionBookingCommand }) }

            coVerify(exactly = 0) { commuteCommandHandler.handle(match { it is CompensateCancelCommuteBookingCommand }) }
            coVerify(exactly = 0) { accommodationCommandHandler.handle(match { it is CompensateCancelAccommodationBookingCommand }) }

            coVerify(exactly = 1) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.PROCESSING },
                    match { it.event is BookingCancelSagaStartedEvent }
                )
            }

            coVerify(exactly = 1) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.COMPLETED },
                    match { it.event is BookingCancelSagaCompletedEvent }
                )
            }

            coVerify(exactly = 0) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.FAILED },
                    match { it.event is BookingCancelSagaFailedEvent }
                )
            }
        }

    @Test
    fun `saga success without attraction should complete and not call attraction handler`() =
        runTest {
            // Given
            val triggering = bookingCancelRequestedEvent(attractionId = AttractionId.Empty)

            val sagaState = SagaState(
                id = UUID.randomUUID(),
                type = SagaType.CANCELLING,
                travelOffer = triggering.travelOffer,
                bookingId = triggering.bookingId,
                seat = triggering.seat,
            )

            val saga = CancelBookingSaga(
                commandBus,
                sagaRepository,
                sagaOutboxPort,
                dlqRepository,
                sagaState,
                metadata)


            // When
            saga.executeOrResume()


            // Then
            coVerify(exactly = 1) { commuteCommandHandler.handle(match { it is CancelCommuteBookingCommand }) }
            coVerify(exactly = 1) { accommodationCommandHandler.handle(match { it is CancelAccommodationBookingCommand }) }
            coVerify(exactly = 0) { attractionCommandHandler.handle(match { it is CancelAttractionBookingCommand }) }

            coVerify(exactly = 0) { commuteCommandHandler.handle(match { it is CompensateCancelCommuteBookingCommand }) }
            coVerify(exactly = 0) { accommodationCommandHandler.handle(match { it is CompensateCancelAccommodationBookingCommand }) }

            coVerify(exactly = 1) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.PROCESSING },
                    match { it.event is BookingCancelSagaStartedEvent }
                )
            }

            coVerify(exactly = 1) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.COMPLETED },
                    match { it.event is BookingCancelSagaCompletedEvent }
                )
            }

            coVerify(exactly = 0) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.FAILED },
                    match { it.event is BookingCancelSagaFailedEvent }
                )
            }
        }

    @Test
    fun `saga commute failure should publish failed and stop`() =
        runTest {
            // Given
            val triggering = bookingCancelRequestedEvent()

            val sagaState = SagaState(
                id = UUID.randomUUID(),
                type = SagaType.CANCELLING,
                travelOffer = triggering.travelOffer,
                bookingId = triggering.bookingId,
                seat = triggering.seat,
            )

            val saga = CancelBookingSaga(
                commandBus,
                sagaRepository,
                sagaOutboxPort,
                dlqRepository,
                sagaState,
                metadata)


            coEvery {
                commuteCommandHandler.handle(match { it is CancelCommuteBookingCommand })
            } throws CommuteException("Commute booking failed")

            // When
            saga.executeOrResume()


            // Then
            coVerify(exactly = 1) { commuteCommandHandler.handle(match { it is CancelCommuteBookingCommand }) }
            coVerify(exactly = 0) { accommodationCommandHandler.handle(match { it is CancelAccommodationBookingCommand }) }
            coVerify(exactly = 0) { attractionCommandHandler.handle(match { it is CancelAttractionBookingCommand }) }

            coVerify(exactly = 0) { commuteCommandHandler.handle(match { it is CompensateCancelCommuteBookingCommand }) }
            coVerify(exactly = 0) { accommodationCommandHandler.handle(match { it is CompensateCancelAccommodationBookingCommand }) }

            coVerify(exactly = 1) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.PROCESSING },
                    match { it.event is BookingCancelSagaStartedEvent }
                )
            }

            coVerify(exactly = 0) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.COMPLETED },
                    match { it.event is BookingCancelSagaCompletedEvent }
                )
            }

            coVerify(exactly = 1) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.FAILED },
                    match { it.event is BookingCancelSagaFailedEvent }
                )
            }
        }

    @Test
    fun `saga accommodation failure should compensate commute and publish failed`() =
        runTest {
            // Given
            val triggering = bookingCancelRequestedEvent(attractionId = AttractionId.Empty)

            coEvery {
                accommodationCommandHandler.handle(match { it is CancelAccommodationBookingCommand })
            } throws AccommodationException("Accommodation booking failed")

            val sagaState = SagaState(
                id = UUID.randomUUID(),
                type = SagaType.CANCELLING,
                travelOffer = triggering.travelOffer,
                bookingId = triggering.bookingId,
                seat = triggering.seat,
            )

            val saga = CancelBookingSaga(
                commandBus,
                sagaRepository,
                sagaOutboxPort,
                dlqRepository,
                sagaState,
                metadata)


            // When
            saga.executeOrResume()


            // Then
            coVerify(exactly = 1) { commuteCommandHandler.handle(match { it is CancelCommuteBookingCommand }) }
            coVerify(exactly = 1) { accommodationCommandHandler.handle(match { it is CancelAccommodationBookingCommand }) }
            coVerify(exactly = 0) { attractionCommandHandler.handle(match { it is CancelAttractionBookingCommand }) }

            coVerify(exactly = 1) { commuteCommandHandler.handle(match { it is CompensateCancelCommuteBookingCommand }) }
            coVerify(exactly = 0) { accommodationCommandHandler.handle(match { it is CompensateCancelAccommodationBookingCommand }) }

            coVerify(exactly = 1) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.PROCESSING },
                    match { it.event is BookingCancelSagaStartedEvent }
                )
            }

            coVerify(exactly = 0) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.COMPLETED },
                    match { it.event is BookingCancelSagaCompletedEvent }
                )
            }

            coVerify(exactly = 1) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.FAILED },
                    match { it.event is BookingCancelSagaFailedEvent }
                )
            }
        }

    @Test
    fun `saga attraction failure should compensate accommodation and commute and publish failed`() =
        runTest {
            // Given
            val triggering = bookingCancelRequestedEvent(attractionId = AttractionId.generate())

            coEvery {
                attractionCommandHandler.handle(match { it is CancelAttractionBookingCommand })
            } throws AttractionException("Attraction booking failed")

            val sagaState = SagaState(
                type = SagaType.CANCELLING,
                travelOffer = triggering.travelOffer,
                bookingId = triggering.bookingId,
                seat = triggering.seat,
            )

            val saga = CancelBookingSaga(
                commandBus,
                sagaRepository,
                sagaOutboxPort,
                dlqRepository,
                sagaState,
                metadata)


            // When
            saga.executeOrResume()


            // Then
            coVerify(exactly = 1) { commuteCommandHandler.handle(match { it is CancelCommuteBookingCommand }) }
            coVerify(exactly = 1) { accommodationCommandHandler.handle(match { it is CancelAccommodationBookingCommand }) }
            coVerify(exactly = 1) { attractionCommandHandler.handle(match { it is CancelAttractionBookingCommand }) }

            coVerify(exactly = 1) { commuteCommandHandler.handle(match { it is CompensateCancelCommuteBookingCommand }) }
            coVerify(exactly = 1) { accommodationCommandHandler.handle(match { it is CompensateCancelAccommodationBookingCommand }) }

            coVerify(exactly = 1) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.PROCESSING },
                    match { it.event is BookingCancelSagaStartedEvent }
                )
            }

            coVerify(exactly = 0) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.COMPLETED },
                    match { it.event is BookingCancelSagaCompletedEvent }
                )
            }

            coVerify(exactly = 1) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.FAILED },
                    match { it.event is BookingCancelSagaFailedEvent }
                )
            }
        }

    @Test
    fun `saga should continue booking from pending commute step`() =
        runTest {
            // Given
            val triggering = bookingCancelRequestedEvent(attractionId = AttractionId.generate())

            val sagaState = SagaState(
                type = SagaType.CANCELLING,
                travelOffer = triggering.travelOffer,
                bookingId = triggering.bookingId,
                seat = triggering.seat,
                status = SagaStatus.PROCESSING,
                step = SagaStep.PENDING_COMMUTE
            )

            val saga = CancelBookingSaga(
                commandBus,
                sagaRepository,
                sagaOutboxPort,
                dlqRepository,
                sagaState,
                metadata)


            // When
            saga.executeOrResume()


            // Then
            coVerify(exactly = 1) { commuteCommandHandler.handle(match { it is CancelCommuteBookingCommand }) }
            coVerify(exactly = 1) { accommodationCommandHandler.handle(match { it is CancelAccommodationBookingCommand }) }
            coVerify(exactly = 1) { attractionCommandHandler.handle(match { it is CancelAttractionBookingCommand }) }

            coVerify(exactly = 0) { commuteCommandHandler.handle(match { it is CompensateCancelCommuteBookingCommand }) }
            coVerify(exactly = 0) { accommodationCommandHandler.handle(match { it is CompensateCancelAccommodationBookingCommand }) }

            coVerify(exactly = 0) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.PROCESSING },
                    match { it.event is BookingCancelSagaStartedEvent }
                )
            }

            coVerify(exactly = 1) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.COMPLETED },
                    match { it.event is BookingCancelSagaCompletedEvent }
                )
            }

            coVerify(exactly = 0) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.FAILED },
                    match { it.event is BookingCancelSagaFailedEvent }
                )
            }
        }

    @Test
    fun `saga should continue booking from pending accommodation step`() =
        runTest {
            // Given
            val triggering = bookingCancelRequestedEvent(attractionId = AttractionId.generate())

            val sagaState = SagaState(
                type = SagaType.CANCELLING,
                travelOffer = triggering.travelOffer,
                bookingId = triggering.bookingId,
                seat = triggering.seat,
                status = SagaStatus.PROCESSING,
                step = SagaStep.PENDING_ACCOMMODATION
            )

            val saga = CancelBookingSaga(
                commandBus,
                sagaRepository,
                sagaOutboxPort,
                dlqRepository,
                sagaState,
                metadata)


            // When
            saga.executeOrResume()


            // Then
            coVerify(exactly = 0) { commuteCommandHandler.handle(match { it is CancelCommuteBookingCommand }) }
            coVerify(exactly = 1) { accommodationCommandHandler.handle(match { it is CancelAccommodationBookingCommand }) }
            coVerify(exactly = 1) { attractionCommandHandler.handle(match { it is CancelAttractionBookingCommand }) }

            coVerify(exactly = 0) { commuteCommandHandler.handle(match { it is CompensateCancelCommuteBookingCommand }) }
            coVerify(exactly = 0) { accommodationCommandHandler.handle(match { it is CompensateCancelAccommodationBookingCommand }) }

            coVerify(exactly = 0) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.PROCESSING },
                    match { it.event is BookingCancelSagaStartedEvent }
                )
            }

            coVerify(exactly = 1) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.COMPLETED },
                    match { it.event is BookingCancelSagaCompletedEvent }
                )
            }

            coVerify(exactly = 0) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.FAILED },
                    match { it.event is BookingCancelSagaFailedEvent }
                )
            }
        }

    @Test
    fun `saga should continue booking from pending attraction step`() =
        runTest {
            // Given
            val triggering = bookingCancelRequestedEvent(attractionId = AttractionId.generate())

            val sagaState = SagaState(
                type = SagaType.CANCELLING,
                travelOffer = triggering.travelOffer,
                bookingId = triggering.bookingId,
                seat = triggering.seat,
                status = SagaStatus.PROCESSING,
                step = SagaStep.PENDING_ATTRACTION
            )

            val saga = CancelBookingSaga(
                commandBus,
                sagaRepository,
                sagaOutboxPort,
                dlqRepository,
                sagaState,
                metadata)


            // When
            saga.executeOrResume()


            // Then
            coVerify(exactly = 0) { commuteCommandHandler.handle(match { it is CancelCommuteBookingCommand }) }
            coVerify(exactly = 0) { accommodationCommandHandler.handle(match { it is CancelAccommodationBookingCommand }) }
            coVerify(exactly = 1) { attractionCommandHandler.handle(match { it is CancelAttractionBookingCommand }) }

            coVerify(exactly = 0) { commuteCommandHandler.handle(match { it is CompensateCancelCommuteBookingCommand }) }
            coVerify(exactly = 0) { accommodationCommandHandler.handle(match { it is CompensateCancelAccommodationBookingCommand }) }

            coVerify(exactly = 0) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.PROCESSING },
                    match { it.event is BookingCancelSagaStartedEvent }
                )
            }

            coVerify(exactly = 1) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.COMPLETED },
                    match { it.event is BookingCancelSagaCompletedEvent }
                )
            }

            coVerify(exactly = 0) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.FAILED },
                    match { it.event is BookingCancelSagaFailedEvent }
                )
            }
        }

    @Test
    fun `completed saga should not send commands`() =
        runTest {
            // Given
            val triggering = bookingCancelRequestedEvent(attractionId = AttractionId.generate())

            val sagaState = SagaState(
                type = SagaType.CANCELLING,
                travelOffer = triggering.travelOffer,
                bookingId = triggering.bookingId,
                seat = triggering.seat,
                status = SagaStatus.COMPLETED
            )

            val saga = CancelBookingSaga(
                commandBus,
                sagaRepository,
                sagaOutboxPort,
                dlqRepository,
                sagaState,
                metadata)


            // When
            saga.executeOrResume()


            // Then
            coVerify(exactly = 0) { commuteCommandHandler.handle(match { it is CancelCommuteBookingCommand }) }
            coVerify(exactly = 0) { accommodationCommandHandler.handle(match { it is CancelAccommodationBookingCommand }) }
            coVerify(exactly = 0) { attractionCommandHandler.handle(match { it is CancelAttractionBookingCommand }) }

            coVerify(exactly = 0) { commuteCommandHandler.handle(match { it is CompensateCancelCommuteBookingCommand }) }
            coVerify(exactly = 0) { accommodationCommandHandler.handle(match { it is CompensateCancelAccommodationBookingCommand }) }

            coVerify(exactly = 0) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.PROCESSING },
                    match { it.event is BookingCancelSagaStartedEvent }
                )
            }

            coVerify(exactly = 0) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.COMPLETED },
                    match { it.event is BookingCancelSagaCompletedEvent }
                )
            }

            coVerify(exactly = 0) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.FAILED },
                    match { it.event is BookingCancelSagaFailedEvent }
                )
            }
        }

    @Test
    fun `saga should continue compensating accommodation process`() =
        runTest {
            // Given
            val triggering = bookingCancelRequestedEvent(attractionId = AttractionId.generate())

            val sagaState = SagaState(
                type = SagaType.CANCELLING,
                travelOffer = triggering.travelOffer,
                bookingId = triggering.bookingId,
                seat = triggering.seat,
                status = SagaStatus.PROCESSING,
                step = SagaStep.COMPENSATING_ACCOMMODATION,
                message = "Attraction booking failed"
            )

            val saga = CancelBookingSaga(
                commandBus,
                sagaRepository,
                sagaOutboxPort,
                dlqRepository,
                sagaState,
                metadata)


            // When
            saga.executeOrResume()


            // Then
            coVerify(exactly = 0) { commuteCommandHandler.handle(match { it is CancelCommuteBookingCommand }) }
            coVerify(exactly = 0) { accommodationCommandHandler.handle(match { it is CancelAccommodationBookingCommand }) }
            coVerify(exactly = 0) { attractionCommandHandler.handle(match { it is CancelAttractionBookingCommand }) }

            coVerify(exactly = 1) { commuteCommandHandler.handle(match { it is CompensateCancelCommuteBookingCommand }) }
            coVerify(exactly = 1) { accommodationCommandHandler.handle(match { it is CompensateCancelAccommodationBookingCommand }) }

            coVerify(exactly = 0) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.PROCESSING },
                    match { it.event is BookingCancelSagaStartedEvent }
                )
            }

            coVerify(exactly = 0) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.COMPLETED },
                    match { it.event is BookingCancelSagaCompletedEvent }
                )
            }

            coVerify(exactly = 1) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.FAILED },
                    match { it.event is BookingCancelSagaFailedEvent }
                )
            }
        }

    @Test
    fun `saga should save to dlq when unable to compensate`() =
        runTest {
            // Given
            val triggering = bookingCancelRequestedEvent(attractionId = AttractionId.generate())

            val sagaState = SagaState(
                type = SagaType.CANCELLING,
                travelOffer = triggering.travelOffer,
                bookingId = triggering.bookingId,
                seat = triggering.seat,
                status = SagaStatus.PROCESSING,
                step = SagaStep.COMPENSATING_ACCOMMODATION,
                message = "Attraction booking failed"
            )

            val saga = CancelBookingSaga(
                commandBus,
                sagaRepository,
                sagaOutboxPort,
                dlqRepository,
                sagaState,
                metadata)

            coEvery { commuteCommandHandler.handle(any()) } throws Exception("Commute domain exception")
            coEvery { accommodationCommandHandler.handle(any()) } throws Exception("Accommodation domain exception")

            // When
            saga.executeOrResume()


            // Then
            coVerify(exactly = 0) { commuteCommandHandler.handle(match { it is CancelCommuteBookingCommand }) }
            coVerify(exactly = 0) { accommodationCommandHandler.handle(match { it is CancelAccommodationBookingCommand }) }
            coVerify(exactly = 0) { attractionCommandHandler.handle(match { it is CancelAttractionBookingCommand }) }

            coVerify(exactly = 1) { commuteCommandHandler.handle(match { it is CompensateCancelCommuteBookingCommand }) }
            coVerify(exactly = 1) { accommodationCommandHandler.handle(match { it is CompensateCancelAccommodationBookingCommand }) }

            coVerify(exactly = 2)  { dlqRepository.save(any()) }

            coVerify(exactly = 0) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.PROCESSING },
                    match { it.event is BookingCancelSagaStartedEvent }
                )
            }

            coVerify(exactly = 0) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.COMPLETED },
                    match { it.event is BookingCancelSagaCompletedEvent }
                )
            }

            coVerify(exactly = 1) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.FAILED },
                    match { it.event is BookingCancelSagaFailedEvent }
                )
            }
        }

    @Test
    fun `saga should continue compensating commute process`() =
        runTest {
            // Given
            val triggering = bookingCancelRequestedEvent(attractionId = AttractionId.generate())

            val sagaState = SagaState(
                type = SagaType.CANCELLING,
                travelOffer = triggering.travelOffer,
                bookingId = triggering.bookingId,
                seat = triggering.seat,
                status = SagaStatus.PROCESSING,
                step = SagaStep.COMPENSATING_COMMUTE,
                message = "Accommodation booking failed"
            )

            val saga = CancelBookingSaga(
                commandBus,
                sagaRepository,
                sagaOutboxPort,
                dlqRepository,
                sagaState,
                metadata)


            // When
            saga.executeOrResume()


            // Then
            coVerify(exactly = 0) { commuteCommandHandler.handle(match { it is CancelCommuteBookingCommand }) }
            coVerify(exactly = 0) { accommodationCommandHandler.handle(match { it is CancelAccommodationBookingCommand }) }
            coVerify(exactly = 0) { attractionCommandHandler.handle(match { it is CancelAttractionBookingCommand }) }

            coVerify(exactly = 1) { commuteCommandHandler.handle(match { it is CompensateCancelCommuteBookingCommand }) }
            coVerify(exactly = 0) { accommodationCommandHandler.handle(match { it is CompensateCancelAccommodationBookingCommand }) }

            coVerify(exactly = 0) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.PROCESSING },
                    match { it.event is BookingCancelSagaStartedEvent }
                )
            }

            coVerify(exactly = 0) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.COMPLETED },
                    match { it.event is BookingCancelSagaCompletedEvent }
                )
            }

            coVerify(exactly = 1) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.FAILED },
                    match { it.event is BookingCancelSagaFailedEvent }
                )
            }
        }

    @Test
    fun `failed saga should not send commands`() =
        runTest {
            // Given
            val triggering = bookingCancelRequestedEvent(attractionId = AttractionId.generate())

            val sagaState = SagaState(
                type = SagaType.CANCELLING,
                travelOffer = triggering.travelOffer,
                bookingId = triggering.bookingId,
                seat = triggering.seat,
                status = SagaStatus.FAILED
            )

            val saga = CancelBookingSaga(
                commandBus,
                sagaRepository,
                sagaOutboxPort,
                dlqRepository,
                sagaState,
                metadata)


            // When
            saga.executeOrResume()


            // Then
            coVerify(exactly = 0) { commuteCommandHandler.handle(match { it is CancelCommuteBookingCommand }) }
            coVerify(exactly = 0) { accommodationCommandHandler.handle(match { it is CancelAccommodationBookingCommand }) }
            coVerify(exactly = 0) { attractionCommandHandler.handle(match { it is CancelAttractionBookingCommand }) }

            coVerify(exactly = 0) { commuteCommandHandler.handle(match { it is CompensateCancelCommuteBookingCommand }) }
            coVerify(exactly = 0) { accommodationCommandHandler.handle(match { it is CompensateCancelAccommodationBookingCommand }) }

            coVerify(exactly = 0) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.PROCESSING },
                    match { it.event is BookingCancelSagaStartedEvent }
                )
            }

            coVerify(exactly = 0) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.COMPLETED },
                    match { it.event is BookingCancelSagaCompletedEvent }
                )
            }

            coVerify(exactly = 0) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.FAILED },
                    match { it.event is BookingCancelSagaFailedEvent }
                )
            }
        }
}
