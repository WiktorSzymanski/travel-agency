//package pl.szymanski.wiktor.ta.saga
//
//import io.mockk.coEvery
//import io.mockk.coVerify
//import io.mockk.mockk
//import kotlinx.coroutines.test.runTest
//import pl.szymanski.wiktor.ta.DummyCommandBus
//import pl.szymanski.wiktor.ta.Metadata
//import pl.szymanski.wiktor.ta.commands.commute.CommuteCommand
//import pl.szymanski.wiktor.ta.commands.accommodation.book.BookAccommodationCommandHandler
//import pl.szymanski.wiktor.ta.commands.accommodation.cancelBooking.CancelAccommodationBookingCommandHandler
//import pl.szymanski.wiktor.ta.commands.accommodation.create.CreateAccommodationCommandHandler
//import pl.szymanski.wiktor.ta.commands.accommodation.expire.ExpireAccommodationCommandHandler
//import pl.szymanski.wiktor.ta.commands.accommodation.compensateBook.CompensateBookAccommodationCommandHandler
//import pl.szymanski.wiktor.ta.commands.accommodation.compensateCancelBooking.CompensateCancelAccommodationBookingCommandHandler
//import pl.szymanski.wiktor.ta.commands.attraction.AttractionCommandHandler
//import pl.szymanski.wiktor.ta.commands.booking.BookingCommandHandler
//import pl.szymanski.wiktor.ta.commands.commute.CommuteCommandHandler
//import pl.szymanski.wiktor.ta.dlq.DeadLetterQueueRepository
//import pl.szymanski.wiktor.ta.domain.Seat
//import pl.szymanski.wiktor.ta.domain.aggregate.AccommodationId
//import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
//import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
//import pl.szymanski.wiktor.ta.domain.aggregate.Commute
//import pl.szymanski.wiktor.ta.domain.aggregate.CommuteId
//import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
//import pl.szymanski.wiktor.ta.domain.event.CommuteEvent
//import java.util.UUID
//import kotlin.test.BeforeTest
//import kotlin.test.Test
//
//class SagaServiceTest {
//    private val bookingCommandHandler = mockk<BookingCommandHandler>(relaxed = true)
//    private val commuteCommandHandler = mockk<CommuteCommandHandler>()
//    private val attractionCommandHandler = mockk<AttractionCommandHandler>(relaxed = true)
//    private val createAccommodationCommandHandler = mockk<CreateAccommodationCommandHandler>(relaxed = true)
//    private val bookAccommodationCommandHandler = mockk<BookAccommodationCommandHandler>()
//    private val cancelAccommodationBookingCommandHandler = mockk<CancelAccommodationBookingCommandHandler>()
//    private val expireAccommodationCommandHandler = mockk<ExpireAccommodationCommandHandler>(relaxed = true)
//    private val compensateBookAccommodationCommandHandler = mockk<CompensateBookAccommodationCommandHandler>(relaxed = true)
//    private val compensateCancelAccommodationBookingCommandHandler = mockk<CompensateCancelAccommodationBookingCommandHandler>(relaxed = true)
//    private lateinit var commandBus: DummyCommandBus
//    private val sagaRepository = mockk<SagaRepository>()
//    private val sagaOutboxPort = mockk<SagaOutboxPort>(relaxed = true)
//    private val deadLetterQueueRepository = mockk<DeadLetterQueueRepository>(relaxed = true)
//    private lateinit var service: SagaService
//
//    @BeforeTest
//    fun setup() {
//        commandBus = DummyCommandBus(
//            bookingCommandHandler,
//            commuteCommandHandler,
//            attractionCommandHandler,
//            createAccommodationCommandHandler,
//            bookAccommodationCommandHandler,
//            cancelAccommodationBookingCommandHandler,
//            expireAccommodationCommandHandler,
//            compensateBookAccommodationCommandHandler,
//            compensateCancelAccommodationBookingCommandHandler,
//        )
//        service = SagaService(commandBus, sagaRepository, sagaOutboxPort, deadLetterQueueRepository)
//    }
//
//    @Test
//    fun `executePendingSagas loads new and processing sagas and executes them`() = runTest {
//        val newSaga = sagaState(SagaType.BOOKING, SagaStatus.NEW)
//        val processingSaga = sagaState(SagaType.CANCELLING, SagaStatus.PROCESSING, SagaStep.PENDING_COMMUTE)
//
//        coEvery { sagaRepository.findByStatuses(listOf(SagaStatus.NEW, SagaStatus.PROCESSING)) } returns
//            listOf(newSaga, processingSaga)
//        coEvery { sagaRepository.save(any()) } returns Unit
//        coEvery { sagaOutboxPort.saveStateWithEvent(any(), any()) } returns UUID.randomUUID()
//        coEvery { commuteCommandHandler.handle(any<CommuteCommand>()) } returns
//            Triple(mockk<Commute>(relaxed = true), emptyList<CommuteEvent>(), mockk<Metadata>(relaxed = true))
//        coEvery { bookAccommodationCommandHandler.handle(any()) } returns Unit
//        coEvery { cancelAccommodationBookingCommandHandler.handle(any()) } returns Unit
//
//        service.executePendingSagas()
//
//        coVerify(exactly = 1) { sagaRepository.findByStatuses(listOf(SagaStatus.NEW, SagaStatus.PROCESSING)) }
//        coVerify(exactly = 2) { commuteCommandHandler.handle(any<CommuteCommand>()) }
//        coVerify(exactly = 1) { bookAccommodationCommandHandler.handle(any()) }
//        coVerify(exactly = 1) { cancelAccommodationBookingCommandHandler.handle(any()) }
//    }
//
//    private fun sagaState(type: SagaType, status: SagaStatus, step: SagaStep = SagaStep.IDLE) =
//        SagaState(
//            id = UUID.randomUUID(),
//            correlationId = UUID.randomUUID(),
//            type = type,
//            status = status,
//            step = step,
//            travelOffer = TravelOffer(
//                commuteId = CommuteId.generate(),
//                accommodationId = AccommodationId.generate(),
//                attractionId = AttractionId.Empty,
//            ),
//            bookingId = BookingId.generate(),
//            seat = Seat.Any,
//        )
//}
