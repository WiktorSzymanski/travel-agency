package pl.szymanski.wiktor.ta.saga

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import pl.szymanski.wiktor.ta.DummyCommandBus
import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.command.BookAccommodationCommand
import pl.szymanski.wiktor.ta.command.BookAttractionCommand
import pl.szymanski.wiktor.ta.command.BookCommuteCommand
import pl.szymanski.wiktor.ta.command.CompensateBookAccommodationCommand
import pl.szymanski.wiktor.ta.command.CompensateBookCommuteCommand
import pl.szymanski.wiktor.ta.commandhandler.AccommodationCommandHandler
import pl.szymanski.wiktor.ta.commandhandler.AttractionCommandHandler
import pl.szymanski.wiktor.ta.commandhandler.BookingCommandHandler
import pl.szymanski.wiktor.ta.commandhandler.CommuteCommandHandler
import pl.szymanski.wiktor.ta.dlq.DeadLetterQueueRepository
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
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookedCompensatedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookedEvent
import pl.szymanski.wiktor.ta.domain.event.BookingCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookedCompensatedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookedEvent
import pl.szymanski.wiktor.ta.domain.exception.AccommodationException
import pl.szymanski.wiktor.ta.domain.exception.AttractionException
import pl.szymanski.wiktor.ta.domain.exception.CommuteException
import pl.szymanski.wiktor.ta.event.BookingSagaCompletedEvent
import pl.szymanski.wiktor.ta.event.BookingSagaFailedEvent
import pl.szymanski.wiktor.ta.event.BookingSagaStartedEvent
import pl.szymanski.wiktor.ta.outbox.SagaOutboxPort
import java.util.UUID
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

    private fun bookingCreatedEvent(
        accommodationId: AccommodationId = AccommodationId.generate(),
        commuteId: CommuteId = CommuteId.generate(),
        attractionId: AttractionId = AttractionId.generate(),
        bookingId: BookingId = BookingId.generate(),
    ) = BookingCreatedEvent(
        travelOffer = TravelOffer(commuteId, accommodationId, attractionId),
        bookingId = bookingId,
        seat = Seat.Picked("1", "A"),
        userId = UUID.randomUUID(),
        state = BookingState.NEW,
    )

    @Test
    fun `saga success with attraction should complete and publish events`() =
        runTest {
            // Given
            val triggering = bookingCreatedEvent(attractionId = AttractionId.generate())

            val sagaState = SagaState(
                id = UUID.randomUUID(),
                type = SagaType.BOOKING,
                travelOffer = triggering.travelOffer,
                bookingId = triggering.bookingId,
                seat = triggering.seat,
            )

            val saga = BookingSaga(
                commandBus,
                sagaRepository,
                sagaOutboxPort,
                dlqRepository,
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

            coVerify(exactly = 1) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.PROCESSING },
                    match { it.event is BookingSagaStartedEvent }
                )
            }

            coVerify(exactly = 1) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.COMPLETED },
                    match { it.event is BookingSagaCompletedEvent }
                )
            }

            coVerify(exactly = 0) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.FAILED },
                    match { it.event is BookingSagaFailedEvent }
                )
            }
        }

    @Test
    fun `saga success without attraction should complete and not call attraction handler`() =
        runTest {
            // Given
            val triggering = bookingCreatedEvent(attractionId = AttractionId.Empty)

            val sagaState = SagaState(
                id = UUID.randomUUID(),
                type = SagaType.BOOKING,
                travelOffer = triggering.travelOffer,
                bookingId = triggering.bookingId,
                seat = triggering.seat,
            )

            val saga = BookingSaga(
                commandBus,
                sagaRepository,
                sagaOutboxPort,
                dlqRepository,
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

            coVerify(exactly = 1) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.PROCESSING },
                    match { it.event is BookingSagaStartedEvent }
                )
            }

            coVerify(exactly = 1) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.COMPLETED },
                    match { it.event is BookingSagaCompletedEvent }
                )
            }

            coVerify(exactly = 0) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.FAILED },
                    match { it.event is BookingSagaFailedEvent }
                )
            }
        }

    @Test
    fun `saga commute failure should publish failed and stop`() =
        runTest {
            // Given
            val triggering = bookingCreatedEvent()

            val sagaState = SagaState(
                id = UUID.randomUUID(),
                type = SagaType.BOOKING,
                travelOffer = triggering.travelOffer,
                bookingId = triggering.bookingId,
                seat = triggering.seat,
            )

            val saga = BookingSaga(
                commandBus,
                sagaRepository,
                sagaOutboxPort,
                dlqRepository,
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

            coVerify(exactly = 1) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.PROCESSING },
                    match { it.event is BookingSagaStartedEvent }
                )
            }

            coVerify(exactly = 0) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.COMPLETED },
                    match { it.event is BookingSagaCompletedEvent }
                )
            }

            coVerify(exactly = 1) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.FAILED },
                    match { it.event is BookingSagaFailedEvent }
                )
            }
        }

    @Test
    fun `saga accommodation failure should compensate commute and publish failed`() =
        runTest {
            // Given
            val triggering = bookingCreatedEvent(attractionId = AttractionId.Empty)

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

            val saga = BookingSaga(
                commandBus,
                sagaRepository,
                sagaOutboxPort,
                dlqRepository,
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

            coVerify(exactly = 1) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.PROCESSING },
                    match { it.event is BookingSagaStartedEvent }
                )
            }

            coVerify(exactly = 0) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.COMPLETED },
                    match { it.event is BookingSagaCompletedEvent }
                )
            }

            coVerify(exactly = 1) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.FAILED },
                    match { it.event is BookingSagaFailedEvent }
                )
            }
        }

    @Test
    fun `saga attraction failure should compensate accommodation and commute and publish failed`() =
        runTest {
            // Given
            val triggering = bookingCreatedEvent(attractionId = AttractionId.generate())

            coEvery {
                attractionCommandHandler.handle(match { it is BookAttractionCommand })
            } throws AttractionException("Attraction booking failed")

            val sagaState = SagaState(
                type = SagaType.BOOKING,
                travelOffer = triggering.travelOffer,
                bookingId = triggering.bookingId,
                seat = triggering.seat,
            )

            val saga = BookingSaga(
                commandBus,
                sagaRepository,
                sagaOutboxPort,
                dlqRepository,
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

            coVerify(exactly = 1) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.PROCESSING },
                    match { it.event is BookingSagaStartedEvent }
                )
            }

            coVerify(exactly = 0) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.COMPLETED },
                    match { it.event is BookingSagaCompletedEvent }
                )
            }

            coVerify(exactly = 1) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.FAILED },
                    match { it.event is BookingSagaFailedEvent }
                )
            }
        }

    @Test
    fun `saga should continue booking from pending commute step`() =
        runTest {
            // Given
            val triggering = bookingCreatedEvent(attractionId = AttractionId.generate())

            val sagaState = SagaState(
                type = SagaType.BOOKING,
                travelOffer = triggering.travelOffer,
                bookingId = triggering.bookingId,
                seat = triggering.seat,
                status = SagaStatus.PROCESSING,
                step = SagaStep.PENDING_COMMUTE
            )

            val saga = BookingSaga(
                commandBus,
                sagaRepository,
                sagaOutboxPort,
                dlqRepository,
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

            coVerify(exactly = 0) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.PROCESSING },
                    match { it.event is BookingSagaStartedEvent }
                )
            }

            coVerify(exactly = 1) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.COMPLETED },
                    match { it.event is BookingSagaCompletedEvent }
                )
            }

            coVerify(exactly = 0) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.FAILED },
                    match { it.event is BookingSagaFailedEvent }
                )
            }
        }

    @Test
    fun `saga should continue booking from pending accommodation step`() =
        runTest {
            // Given
            val triggering = bookingCreatedEvent(attractionId = AttractionId.generate())

            val sagaState = SagaState(
                type = SagaType.BOOKING,
                travelOffer = triggering.travelOffer,
                bookingId = triggering.bookingId,
                seat = triggering.seat,
                status = SagaStatus.PROCESSING,
                step = SagaStep.PENDING_ACCOMMODATION
            )

            val saga = BookingSaga(
                commandBus,
                sagaRepository,
                sagaOutboxPort,
                dlqRepository,
                sagaState,
                metadata)


            // When
            saga.executeOrResume()


            // Then
            coVerify(exactly = 0) { commuteCommandHandler.handle(match { it is BookCommuteCommand }) }
            coVerify(exactly = 1) { accommodationCommandHandler.handle(match { it is BookAccommodationCommand }) }
            coVerify(exactly = 1) { attractionCommandHandler.handle(match { it is BookAttractionCommand }) }

            coVerify(exactly = 0) { commuteCommandHandler.handle(match { it is CompensateBookCommuteCommand }) }
            coVerify(exactly = 0) { accommodationCommandHandler.handle(match { it is CompensateBookAccommodationCommand }) }

            coVerify(exactly = 0) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.PROCESSING },
                    match { it.event is BookingSagaStartedEvent }
                )
            }

            coVerify(exactly = 1) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.COMPLETED },
                    match { it.event is BookingSagaCompletedEvent }
                )
            }

            coVerify(exactly = 0) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.FAILED },
                    match { it.event is BookingSagaFailedEvent }
                )
            }
        }

    @Test
    fun `saga should continue booking from pending attraction step`() =
        runTest {
            // Given
            val triggering = bookingCreatedEvent(attractionId = AttractionId.generate())

            val sagaState = SagaState(
                type = SagaType.BOOKING,
                travelOffer = triggering.travelOffer,
                bookingId = triggering.bookingId,
                seat = triggering.seat,
                status = SagaStatus.PROCESSING,
                step = SagaStep.PENDING_ATTRACTION
            )

            val saga = BookingSaga(
                commandBus,
                sagaRepository,
                sagaOutboxPort,
                dlqRepository,
                sagaState,
                metadata)


            // When
            saga.executeOrResume()


            // Then
            coVerify(exactly = 0) { commuteCommandHandler.handle(match { it is BookCommuteCommand }) }
            coVerify(exactly = 0) { accommodationCommandHandler.handle(match { it is BookAccommodationCommand }) }
            coVerify(exactly = 1) { attractionCommandHandler.handle(match { it is BookAttractionCommand }) }

            coVerify(exactly = 0) { commuteCommandHandler.handle(match { it is CompensateBookCommuteCommand }) }
            coVerify(exactly = 0) { accommodationCommandHandler.handle(match { it is CompensateBookAccommodationCommand }) }

            coVerify(exactly = 0) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.PROCESSING },
                    match { it.event is BookingSagaStartedEvent }
                )
            }

            coVerify(exactly = 1) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.COMPLETED },
                    match { it.event is BookingSagaCompletedEvent }
                )
            }

            coVerify(exactly = 0) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.FAILED },
                    match { it.event is BookingSagaFailedEvent }
                )
            }
        }

    @Test
    fun `completed saga should not send commands`() =
        runTest {
            // Given
            val triggering = bookingCreatedEvent(attractionId = AttractionId.generate())

            val sagaState = SagaState(
                type = SagaType.BOOKING,
                travelOffer = triggering.travelOffer,
                bookingId = triggering.bookingId,
                seat = triggering.seat,
                status = SagaStatus.COMPLETED
            )

            val saga = BookingSaga(
                commandBus,
                sagaRepository,
                sagaOutboxPort,
                dlqRepository,
                sagaState,
                metadata)


            // When
            saga.executeOrResume()


            // Then
            coVerify(exactly = 0) { commuteCommandHandler.handle(match { it is BookCommuteCommand }) }
            coVerify(exactly = 0) { accommodationCommandHandler.handle(match { it is BookAccommodationCommand }) }
            coVerify(exactly = 0) { attractionCommandHandler.handle(match { it is BookAttractionCommand }) }

            coVerify(exactly = 0) { commuteCommandHandler.handle(match { it is CompensateBookCommuteCommand }) }
            coVerify(exactly = 0) { accommodationCommandHandler.handle(match { it is CompensateBookAccommodationCommand }) }

            coVerify(exactly = 0) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.PROCESSING },
                    match { it.event is BookingSagaStartedEvent }
                )
            }

            coVerify(exactly = 0) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.COMPLETED },
                    match { it.event is BookingSagaCompletedEvent }
                )
            }

            coVerify(exactly = 0) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.FAILED },
                    match { it.event is BookingSagaFailedEvent }
                )
            }
        }

    @Test
    fun `saga should continue compensating accommodation process`() =
        runTest {
            // Given
            val triggering = bookingCreatedEvent(attractionId = AttractionId.generate())

            val sagaState = SagaState(
                type = SagaType.BOOKING,
                travelOffer = triggering.travelOffer,
                bookingId = triggering.bookingId,
                seat = triggering.seat,
                status = SagaStatus.PROCESSING,
                step = SagaStep.COMPENSATING_ACCOMMODATION,
                message = "Attraction booking failed"
            )

            val saga = BookingSaga(
                commandBus,
                sagaRepository,
                sagaOutboxPort,
                dlqRepository,
                sagaState,
                metadata)


            // When
            saga.executeOrResume()


            // Then
            coVerify(exactly = 0) { commuteCommandHandler.handle(match { it is BookCommuteCommand }) }
            coVerify(exactly = 0) { accommodationCommandHandler.handle(match { it is BookAccommodationCommand }) }
            coVerify(exactly = 0) { attractionCommandHandler.handle(match { it is BookAttractionCommand }) }

            coVerify(exactly = 1) { commuteCommandHandler.handle(match { it is CompensateBookCommuteCommand }) }
            coVerify(exactly = 1) { accommodationCommandHandler.handle(match { it is CompensateBookAccommodationCommand }) }

            coVerify(exactly = 0) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.PROCESSING },
                    match { it.event is BookingSagaStartedEvent }
                )
            }

            coVerify(exactly = 0) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.COMPLETED },
                    match { it.event is BookingSagaCompletedEvent }
                )
            }

            coVerify(exactly = 1) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.FAILED },
                    match { it.event is BookingSagaFailedEvent }
                )
            }
        }

    @Test
    fun `saga should save to dlq when unable to compensate`() =
        runTest {
            // Given
            val triggering = bookingCreatedEvent(attractionId = AttractionId.generate())

            val sagaState = SagaState(
                type = SagaType.BOOKING,
                travelOffer = triggering.travelOffer,
                bookingId = triggering.bookingId,
                seat = triggering.seat,
                status = SagaStatus.PROCESSING,
                step = SagaStep.COMPENSATING_ACCOMMODATION,
                message = "Attraction booking failed"
            )

            val saga = BookingSaga(
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
            coVerify(exactly = 0) { commuteCommandHandler.handle(match { it is BookCommuteCommand }) }
            coVerify(exactly = 0) { accommodationCommandHandler.handle(match { it is BookAccommodationCommand }) }
            coVerify(exactly = 0) { attractionCommandHandler.handle(match { it is BookAttractionCommand }) }

            coVerify(exactly = 1) { commuteCommandHandler.handle(match { it is CompensateBookCommuteCommand }) }
            coVerify(exactly = 1) { accommodationCommandHandler.handle(match { it is CompensateBookAccommodationCommand }) }

            coVerify(exactly = 2)  { dlqRepository.save(any()) }

            coVerify(exactly = 0) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.PROCESSING },
                    match { it.event is BookingSagaStartedEvent }
                )
            }

            coVerify(exactly = 0) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.COMPLETED },
                    match { it.event is BookingSagaCompletedEvent }
                )
            }

            coVerify(exactly = 1) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.FAILED },
                    match { it.event is BookingSagaFailedEvent }
                )
            }
        }

    @Test
    fun `saga should continue compensating commute process`() =
        runTest {
            // Given
            val triggering = bookingCreatedEvent(attractionId = AttractionId.generate())

            val sagaState = SagaState(
                type = SagaType.BOOKING,
                travelOffer = triggering.travelOffer,
                bookingId = triggering.bookingId,
                seat = triggering.seat,
                status = SagaStatus.PROCESSING,
                step = SagaStep.COMPENSATING_COMMUTE,
                message = "Accommodation booking failed"
            )

            val saga = BookingSaga(
                commandBus,
                sagaRepository,
                sagaOutboxPort,
                dlqRepository,
                sagaState,
                metadata)


            // When
            saga.executeOrResume()


            // Then
            coVerify(exactly = 0) { commuteCommandHandler.handle(match { it is BookCommuteCommand }) }
            coVerify(exactly = 0) { accommodationCommandHandler.handle(match { it is BookAccommodationCommand }) }
            coVerify(exactly = 0) { attractionCommandHandler.handle(match { it is BookAttractionCommand }) }

            coVerify(exactly = 1) { commuteCommandHandler.handle(match { it is CompensateBookCommuteCommand }) }
            coVerify(exactly = 0) { accommodationCommandHandler.handle(match { it is CompensateBookAccommodationCommand }) }

            coVerify(exactly = 0) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.PROCESSING },
                    match { it.event is BookingSagaStartedEvent }
                )
            }

            coVerify(exactly = 0) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.COMPLETED },
                    match { it.event is BookingSagaCompletedEvent }
                )
            }

            coVerify(exactly = 1) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.FAILED },
                    match { it.event is BookingSagaFailedEvent }
                )
            }
        }

    @Test
    fun `failed saga should not send commands`() =
        runTest {
            // Given
            val triggering = bookingCreatedEvent(attractionId = AttractionId.generate())

            val sagaState = SagaState(
                type = SagaType.BOOKING,
                travelOffer = triggering.travelOffer,
                bookingId = triggering.bookingId,
                seat = triggering.seat,
                status = SagaStatus.FAILED
            )

            val saga = BookingSaga(
                commandBus,
                sagaRepository,
                sagaOutboxPort,
                dlqRepository,
                sagaState,
                metadata)


            // When
            saga.executeOrResume()


            // Then
            coVerify(exactly = 0) { commuteCommandHandler.handle(match { it is BookCommuteCommand }) }
            coVerify(exactly = 0) { accommodationCommandHandler.handle(match { it is BookAccommodationCommand }) }
            coVerify(exactly = 0) { attractionCommandHandler.handle(match { it is BookAttractionCommand }) }

            coVerify(exactly = 0) { commuteCommandHandler.handle(match { it is CompensateBookCommuteCommand }) }
            coVerify(exactly = 0) { accommodationCommandHandler.handle(match { it is CompensateBookAccommodationCommand }) }

            coVerify(exactly = 0) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.PROCESSING },
                    match { it.event is BookingSagaStartedEvent }
                )
            }

            coVerify(exactly = 0) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.COMPLETED },
                    match { it.event is BookingSagaCompletedEvent }
                )
            }

            coVerify(exactly = 0) {
                sagaOutboxPort.saveStateWithEvent(
                    match { it.status == SagaStatus.FAILED },
                    match { it.event is BookingSagaFailedEvent }
                )
            }
        }
}
