package pl.szymanski.wiktor.ta.eventHandlerLogic

import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import pl.szymanski.wiktor.ta.CommandBus
import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.command.BookingCommand
import pl.szymanski.wiktor.ta.command.CancelBookTravelOfferCommand
import pl.szymanski.wiktor.ta.command.CancelBookingCommand
import pl.szymanski.wiktor.ta.command.FailCancelBookingCommand
import pl.szymanski.wiktor.ta.command.ProcessCancelBookingCommand
import pl.szymanski.wiktor.ta.command.TravelOfferCommand
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.aggregate.Booking
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOfferId
import pl.szymanski.wiktor.ta.event.BookingCancelSagaCompletedEvent
import pl.szymanski.wiktor.ta.event.BookingCancelSagaFailedEvent
import pl.szymanski.wiktor.ta.event.BookingCancelSagaStartedEvent
import java.util.UUID
import kotlin.test.Test

class BookingCancelSagaEventHandleLogicTest {
    private val commandBus = mockk<CommandBus>(relaxed = true)
    private val logic = BookingCancelSagaEventHandleLogic(commandBus)

    @Test
    fun `should handle BookingCancelSagaStartedEvent`() = runTest {
        val bookingId = BookingId.generate()
        val correlationId = UUID.randomUUID()

        logic.onStartedEvent(EventEnvelope(
            BookingCancelSagaStartedEvent(
                bookingId = bookingId,
            ),
            Metadata(
                correlationId,
                0
            )
        ))

        coVerify {
            commandBus.dispatch<BookingCommand, Booking>(
                ProcessCancelBookingCommand(
                    bookingId,
                    correlationId,
                )
            )
        }
    }

    @Test
    fun `should handle BookingCancelSagaCompletedEvent`() = runTest {
        val bookingId = BookingId.generate()
        val travelOfferId = TravelOfferId.from(UUID.randomUUID())
        val correlationId = UUID.randomUUID()
        val seat = Seat.Any

        logic.onCompletedEvent(EventEnvelope(
            BookingCancelSagaCompletedEvent(
                bookingId = bookingId,
                travelOfferId = travelOfferId,
                seat = seat
            ),
            Metadata(
                correlationId,
                0
            )
        ))

        coVerify {
            commandBus.dispatch<BookingCommand, Booking>(
                CancelBookingCommand(
                    bookingId,
                    correlationId,
                )
            )
        }
    }

    @Test
    fun `should handle BookingCancelSagaCompletedEvent2`() = runTest {
        val bookingId = BookingId.generate()
        val travelOfferId = TravelOfferId.from(UUID.randomUUID())
        val correlationId = UUID.randomUUID()
        val seat = Seat.Any

        logic.onCompletedEvent2(EventEnvelope(
            BookingCancelSagaCompletedEvent(
                bookingId = bookingId,
                travelOfferId = travelOfferId,
                seat = seat
            ),
            Metadata(
                correlationId,
                0
            )
        ))

        coVerify {
            commandBus.dispatch<TravelOfferCommand, TravelOffer>(
                CancelBookTravelOfferCommand(
                    travelOfferId,
                    correlationId,
                    bookingId = bookingId,
                    seat = seat,
                )
            )
        }
    }

    @Test
    fun `should handle BookingCancelSagaFailedEvent`() = runTest {
        val bookingId = BookingId.generate()
        val correlationId = UUID.randomUUID()
        val message = "Error"

        logic.onFailedEvent(EventEnvelope(
            BookingCancelSagaFailedEvent(
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
                FailCancelBookingCommand(
                    bookingId = bookingId,
                    correlationId = correlationId,
                    message = message,
                )
            )
        }
    }
}
