package pl.szymanski.wiktor.ta.saga

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import pl.szymanski.wiktor.ta.DummyCommandBus
import pl.szymanski.wiktor.ta.command.AccommodationCommand
import pl.szymanski.wiktor.ta.command.CommuteCommand
import pl.szymanski.wiktor.ta.commandhandler.AccommodationCommandHandler
import pl.szymanski.wiktor.ta.commandhandler.AttractionCommandHandler
import pl.szymanski.wiktor.ta.commandhandler.BookingCommandHandler
import pl.szymanski.wiktor.ta.commandhandler.CommuteCommandHandler
import pl.szymanski.wiktor.ta.dlq.DeadLetterQueueRepository
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.AccommodationId
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.aggregate.CommuteId
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import pl.szymanski.wiktor.ta.domain.event.AccommodationEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteEvent
import pl.szymanski.wiktor.ta.outbox.SagaOutboxPort
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test

class SagaServiceTest {
    private val bookingCommandHandler = mockk<BookingCommandHandler>(relaxed = true)
    private val commuteCommandHandler = mockk<CommuteCommandHandler>()
    private val attractionCommandHandler = mockk<AttractionCommandHandler>(relaxed = true)
    private val accommodationCommandHandler = mockk<AccommodationCommandHandler>()
    private lateinit var commandBus: DummyCommandBus
    private val sagaRepository = mockk<SagaRepository>()
    private val sagaOutboxPort = mockk<SagaOutboxPort>(relaxed = true)
    private val deadLetterQueueRepository = mockk<DeadLetterQueueRepository>(relaxed = true)
    private lateinit var service: SagaService

    @BeforeTest
    fun setup() {
        commandBus = DummyCommandBus(
            bookingCommandHandler,
            commuteCommandHandler,
            attractionCommandHandler,
            accommodationCommandHandler,
        )
        service = SagaService(commandBus, sagaRepository, sagaOutboxPort, deadLetterQueueRepository)
    }

    @Test
    fun `executePendingSagas loads new and processing sagas and executes them`() = runTest {
        val newSaga = sagaState(SagaType.BOOKING, SagaStatus.NEW)
        val processingSaga = sagaState(SagaType.CANCELLING, SagaStatus.PROCESSING, SagaStep.PENDING_COMMUTE)

        coEvery { sagaRepository.findByStatuses(listOf(SagaStatus.NEW, SagaStatus.PROCESSING)) } returns
            listOf(newSaga, processingSaga)
        coEvery { sagaRepository.save(any()) } returns Unit
        coEvery { sagaOutboxPort.saveStateWithEvent(any(), any()) } returns UUID.randomUUID()
        coEvery { commuteCommandHandler.handle(any<CommuteCommand>()) } returns
            (mockk<Commute>(relaxed = true) to emptyList<CommuteEvent>())
        coEvery { accommodationCommandHandler.handle(any<AccommodationCommand>()) } returns
            (mockk<Accommodation>(relaxed = true) to emptyList<AccommodationEvent>())

        service.executePendingSagas()

        coVerify(exactly = 1) { sagaRepository.findByStatuses(listOf(SagaStatus.NEW, SagaStatus.PROCESSING)) }
        coVerify(exactly = 2) { commuteCommandHandler.handle(any<CommuteCommand>()) }
        coVerify(exactly = 2) { accommodationCommandHandler.handle(any<AccommodationCommand>()) }
    }

    private fun sagaState(type: SagaType, status: SagaStatus, step: SagaStep = SagaStep.IDLE) =
        SagaState(
            id = UUID.randomUUID(),
            correlationId = UUID.randomUUID(),
            type = type,
            status = status,
            step = step,
            travelOffer = TravelOffer(
                commuteId = CommuteId.generate(),
                accommodationId = AccommodationId.generate(),
                attractionId = AttractionId.Empty,
            ),
            bookingId = BookingId.generate(),
            seat = Seat.Any,
        )
}
