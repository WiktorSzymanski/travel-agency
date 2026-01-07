package pl.szymanski.wiktor.ta.eventHandlerLogic

import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import pl.szymanski.wiktor.ta.CommandBus
import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.command.BookingCommand
import pl.szymanski.wiktor.ta.command.CompleteBookingCommand
import pl.szymanski.wiktor.ta.command.FailBookingCommand
import pl.szymanski.wiktor.ta.command.ProcessBookingCommand
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.aggregate.Booking
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import pl.szymanski.wiktor.ta.event.BookingSagaCompletedEvent
import pl.szymanski.wiktor.ta.event.BookingSagaFailedEvent
import pl.szymanski.wiktor.ta.event.BookingSagaStartedEvent
import java.util.UUID
import kotlin.test.Test

class BookingSagaEventHandleLogicTest {
    private val commandBus = mockk<CommandBus>(relaxed = true)
    private val logic = BookingSagaEventHandleLogic(commandBus)

    @Test
    fun `should handle BookingSagaStartedEvent`() = runTest {
        val bookingId = BookingId.generate()
        val correlationId = UUID.randomUUID()

        logic.onStartedEvent(EventEnvelope(
            BookingSagaStartedEvent(
                bookingId = bookingId,
            ),
            Metadata(
                correlationId,
                0
            )
        ))

        coVerify {
            commandBus.dispatch<BookingCommand, Booking>(
                ProcessBookingCommand(
                    bookingId,
                    correlationId,
                )
            )
        }
    }

    @Test
    fun `should handle BookingSagaCompletedEvent`() = runTest {
        val bookingId = BookingId.generate()
        val correlationId = UUID.randomUUID()
        val seat = Seat.Any

        logic.onCompletedEvent(EventEnvelope(
            BookingSagaCompletedEvent(
                bookingId = bookingId,
                seat = seat
            ),
            Metadata(
                correlationId,
                0
            )
        ))

        coVerify {
            commandBus.dispatch<BookingCommand, Booking>(
                CompleteBookingCommand(
                    bookingId,
                    correlationId,
                )
            )
        }
    }

    @Test
    fun `should handle BookingSagaFailedEvent`() = runTest {
        val bookingId = BookingId.generate()
        val correlationId = UUID.randomUUID()
        val message = "Error"

        logic.onFailedEvent(EventEnvelope(
            BookingSagaFailedEvent(
                bookingId = bookingId,
                message = message,
            ),
            Metadata(
                correlationId,
                0
            )
        ))

        coVerify {
            commandBus.dispatch<BookingCommand, Booking>(
                FailBookingCommand(
                    bookingId = bookingId,
                    correlationId = correlationId,
                    message = message,
                )
            )
        }
    }
}
