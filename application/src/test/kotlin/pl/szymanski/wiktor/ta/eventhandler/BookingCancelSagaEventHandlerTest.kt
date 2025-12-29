package pl.szymanski.wiktor.ta.eventhandler

import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import pl.szymanski.wiktor.ta.CommandBus
import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.command.CancelBookTravelOfferCommand
import pl.szymanski.wiktor.ta.command.CancelBookingCommand
import pl.szymanski.wiktor.ta.command.FailCancelBookingCommand
import pl.szymanski.wiktor.ta.command.ProcessCancelBookingCommand
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.aggregate.Booking
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import pl.szymanski.wiktor.ta.event.BookingCancelSagaCompletedEvent
import pl.szymanski.wiktor.ta.event.BookingCancelSagaFailedEvent
import pl.szymanski.wiktor.ta.event.BookingCancelSagaStartedEvent
import pl.szymanski.wiktor.ta.saga.DummyEventBus
import java.util.UUID
import kotlin.test.Test

class BookingCancelSagaEventHandlerTest {
    private val eventBus = DummyEventBus()
    private val commandBus = mockk<CommandBus>(relaxed = true)

    @Test
    fun `should handle BookingCancelSagaStartedEvent`() = runTest(UnconfinedTestDispatcher()) {
        BookingCancelSagaEventHandler(eventBus, commandBus, backgroundScope)

        val bookingId = UUID.randomUUID()
        val correlationId = UUID.randomUUID()

        eventBus.publish(EventEnvelope(
            BookingCancelSagaStartedEvent(
                bookingId = bookingId,
            ),
            Metadata(
                correlationId,
                0
            )
        ))

        coVerify {
            commandBus.dispatch<ProcessCancelBookingCommand, Booking>(
                ProcessCancelBookingCommand(
                    bookingId = bookingId,
                    correlationId = correlationId,
                )
            )
        }
    }

    @Test
    fun `should handle BookingCancelSagaCompletedEvent`() = runTest(UnconfinedTestDispatcher()) {
        BookingCancelSagaEventHandler(eventBus, commandBus, backgroundScope)

        val bookingId = UUID.randomUUID()
        val travelOfferId = UUID.randomUUID()
        val correlationId = UUID.randomUUID()
        val seat = Seat.Any

        eventBus.publish(EventEnvelope(
            BookingCancelSagaCompletedEvent(
                bookingId = bookingId,
                travelOfferId = travelOfferId,
                seat = seat,
            ),
            Metadata(
                correlationId,
                0
            )
        ))

        coVerify {
            commandBus.dispatch<CancelBookingCommand, Booking>(
                CancelBookingCommand(
                    bookingId = bookingId,
                    correlationId = correlationId,
                )
            )
        }

        coVerify {
            commandBus.dispatch<CancelBookTravelOfferCommand, TravelOffer>(
                CancelBookTravelOfferCommand(
                    travelOfferId = travelOfferId,
                    correlationId = correlationId,
                    bookingId = bookingId,
                    seat = seat,
                )
            )
        }
    }

    @Test
    fun `should handle BookingCancelSagaFailedEvent`() = runTest(UnconfinedTestDispatcher()) {
        BookingCancelSagaEventHandler(eventBus, commandBus, backgroundScope)

        val bookingId = UUID.randomUUID()
        val correlationId = UUID.randomUUID()
        val message = "Error message"

        eventBus.publish(EventEnvelope(
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
            commandBus.dispatch<FailCancelBookingCommand, Booking>(
                FailCancelBookingCommand(
                    bookingId = bookingId,
                    correlationId = correlationId,
                    message = message,
                )
            )
        }
    }
}
