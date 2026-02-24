//package pl.szymanski.wiktor.ta.eventHandlerLogic
//
//import io.mockk.coVerify
//import io.mockk.mockk
//import kotlinx.coroutines.test.runTest
//import pl.szymanski.wiktor.ta.EventEnvelope
//import pl.szymanski.wiktor.ta.Metadata
//import pl.szymanski.wiktor.ta.commands.booking.cancel.CancelBookingCommand
//import pl.szymanski.wiktor.ta.commands.booking.cancel.CancelBookingCommandHandler
//import pl.szymanski.wiktor.ta.commands.booking.failCancel.FailCancelBookingCommand
//import pl.szymanski.wiktor.ta.commands.booking.failCancel.FailCancelBookingCommandHandler
//import pl.szymanski.wiktor.ta.commands.booking.processCancel.ProcessCancelBookingCommand
//import pl.szymanski.wiktor.ta.commands.booking.processCancel.ProcessCancelBookingCommandHandler
//import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
//import pl.szymanski.wiktor.ta.event.BookingCancelSagaCompletedEvent
//import pl.szymanski.wiktor.ta.event.BookingCancelSagaFailedEvent
//import pl.szymanski.wiktor.ta.event.BookingCancelSagaStartedEvent
//import java.util.UUID
//import kotlin.test.Test
//
//class BookingCancelSagaEventHandleLogicTest {
//    private val processCancelBookingCommandHandler = mockk<ProcessCancelBookingCommandHandler>(relaxed = true)
//    private val cancelBookingCommandHandler = mockk<CancelBookingCommandHandler>(relaxed = true)
//    private val failCancelBookingCommandHandler = mockk<FailCancelBookingCommandHandler>(relaxed = true)
//
//    @Test
//    fun `should handle BookingCancelSagaStartedEvent`() = runTest {
//        val bookingId = BookingId.generate()
//        val correlationId = UUID.randomUUID()
//
//        onBookingCancelSagaStartedEvent(
//            processCancelBookingCommandHandler,
//            EventEnvelope(
//                BookingCancelSagaStartedEvent(bookingId = bookingId),
//                Metadata(correlationId, 0)
//            )
//        )
//
//        coVerify {
//            processCancelBookingCommandHandler.handle(
//                ProcessCancelBookingCommand(
//                    correlationId = correlationId,
//                    bookingId = bookingId,
//                )
//            )
//        }
//    }
//
//    @Test
//    fun `should handle BookingCancelSagaCompletedEvent`() = runTest {
//        val bookingId = BookingId.generate()
//        val correlationId = UUID.randomUUID()
//
//        onBookingCancelSagaCompletedEvent(
//            cancelBookingCommandHandler,
//            EventEnvelope(
//                BookingCancelSagaCompletedEvent(bookingId = bookingId),
//                Metadata(correlationId, 0)
//            )
//        )
//
//        coVerify {
//            cancelBookingCommandHandler.handle(
//                CancelBookingCommand(
//                    correlationId = correlationId,
//                    bookingId = bookingId,
//                )
//            )
//        }
//    }
//
//    @Test
//    fun `should handle BookingCancelSagaFailedEvent`() = runTest {
//        val bookingId = BookingId.generate()
//        val correlationId = UUID.randomUUID()
//        val message = "Error"
//
//        onBookingCancelSagaFailedEvent(
//            failCancelBookingCommandHandler,
//            EventEnvelope(
//                BookingCancelSagaFailedEvent(bookingId = bookingId, message = message),
//                Metadata(correlationId, 0)
//            )
//        )
//
//        coVerify {
//            failCancelBookingCommandHandler.handle(
//                FailCancelBookingCommand(
//                    bookingId = bookingId,
//                    correlationId = correlationId,
//                    message = message,
//                )
//            )
//        }
//    }
//}
