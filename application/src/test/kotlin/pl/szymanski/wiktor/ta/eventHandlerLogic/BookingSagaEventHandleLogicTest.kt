//package pl.szymanski.wiktor.ta.eventHandlerLogic
//
//import io.mockk.coVerify
//import io.mockk.mockk
//import kotlinx.coroutines.test.runTest
//import pl.szymanski.wiktor.ta.EventEnvelope
//import pl.szymanski.wiktor.ta.Metadata
//import pl.szymanski.wiktor.ta.commands.booking.complete.CompleteBookingCommand
//import pl.szymanski.wiktor.ta.commands.booking.complete.CompleteBookingCommandHandler
//import pl.szymanski.wiktor.ta.commands.booking.fail.FailBookingCommand
//import pl.szymanski.wiktor.ta.commands.booking.fail.FailBookingCommandHandler
//import pl.szymanski.wiktor.ta.commands.booking.process.ProcessBookingCommand
//import pl.szymanski.wiktor.ta.commands.booking.process.ProcessBookingCommandHandler
//import pl.szymanski.wiktor.ta.domain.Seat
//import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
//import pl.szymanski.wiktor.ta.event.BookingSagaCompletedEvent
//import pl.szymanski.wiktor.ta.event.BookingSagaFailedEvent
//import pl.szymanski.wiktor.ta.event.BookingSagaStartedEvent
//import java.util.UUID
//import kotlin.test.Test
//
//class BookingSagaEventHandleLogicTest {
//    private val processBookingCommandHandler = mockk<ProcessBookingCommandHandler>(relaxed = true)
//    private val completeBookingCommandHandler = mockk<CompleteBookingCommandHandler>(relaxed = true)
//    private val failBookingCommandHandler = mockk<FailBookingCommandHandler>(relaxed = true)
//
//    @Test
//    fun `should handle BookingSagaStartedEvent`() = runTest {
//        val bookingId = BookingId.generate()
//        val correlationId = UUID.randomUUID()
//
//        onBookingSagaStartedEvent(
//            processBookingCommandHandler,
//            EventEnvelope(
//                BookingSagaStartedEvent(bookingId = bookingId.value!!),
//                Metadata(correlationId, 0)
//            )
//        )
//
//        coVerify {
//            processBookingCommandHandler.handle(
//                ProcessBookingCommand(
//                    correlationId = correlationId,
//                    bookingId = bookingId,
//                )
//            )
//        }
//    }
//
//    @Test
//    fun `should handle BookingSagaCompletedEvent`() = runTest {
//        val bookingId = BookingId.generate()
//        val correlationId = UUID.randomUUID()
//
//        onBookingSagaCompletedEvent(
//            completeBookingCommandHandler,
//            EventEnvelope(
//                BookingSagaCompletedEvent(bookingId = bookingId.value!!, seat = Seat.Any),
//                Metadata(correlationId, 0)
//            )
//        )
//
//        coVerify {
//            completeBookingCommandHandler.handle(
//                CompleteBookingCommand(
//                    correlationId = correlationId,
//                    bookingId = bookingId,
//                )
//            )
//        }
//    }
//
//    @Test
//    fun `should handle BookingSagaFailedEvent`() = runTest {
//        val bookingId = BookingId.generate()
//        val correlationId = UUID.randomUUID()
//        val message = "Error"
//
//        onBookingSagaFailedEvent(
//            failBookingCommandHandler,
//            EventEnvelope(
//                BookingSagaFailedEvent(bookingId = bookingId.value!!, message = message),
//                Metadata(correlationId, 0)
//            )
//        )
//
//        coVerify {
//            failBookingCommandHandler.handle(
//                FailBookingCommand(
//                    bookingId = bookingId,
//                    correlationId = correlationId,
//                    message = message,
//                )
//            )
//        }
//    }
//}
