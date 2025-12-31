package pl.szymanski.wiktor.ta.eventhandler

import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import pl.szymanski.wiktor.ta.CommandBus
import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.command.BookTravelOfferCommand
import pl.szymanski.wiktor.ta.command.CompleteBookingCommand
import pl.szymanski.wiktor.ta.command.FailBookingCommand
import pl.szymanski.wiktor.ta.command.ProcessBookingCommand
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.aggregate.Booking
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOfferId
import pl.szymanski.wiktor.ta.event.BookingSagaCompletedEvent
import pl.szymanski.wiktor.ta.event.BookingSagaFailedEvent
import pl.szymanski.wiktor.ta.event.BookingSagaStartedEvent
import pl.szymanski.wiktor.ta.saga.DummyEventBus
import java.util.UUID
import kotlin.test.Test

class BookingSagaEventHandlerTest {
    private val eventBus = DummyEventBus()
    private val commandBus = mockk<CommandBus>(relaxed = true)

    @Test
    fun `should handle BookingSagaStartedEvent`() = runTest(UnconfinedTestDispatcher()) {
        BookingSagaEventHandler(eventBus, commandBus, backgroundScope)

        val bookingId = BookingId.generate()
        val correlationId = UUID.randomUUID()

        eventBus.publish(EventEnvelope(
            BookingSagaStartedEvent(
                bookingId = bookingId,
            ),
            Metadata(
                correlationId,
                0
            )
        ))

        coVerify {
            commandBus.dispatch<ProcessBookingCommand, Booking>(
                ProcessBookingCommand(
                    bookingId = bookingId,
                    correlationId = correlationId,
                )
            )
        }
    }

    @Test
    fun `should handle BookingSagaCompletedEvent`() = runTest(UnconfinedTestDispatcher()) {
        BookingSagaEventHandler(eventBus, commandBus, backgroundScope)

        val bookingId = BookingId.generate()
        val travelOfferId = TravelOfferId.from(UUID.randomUUID())
        val correlationId = UUID.randomUUID()
        val seat = Seat.Any

        eventBus.publish(EventEnvelope(
            BookingSagaCompletedEvent(
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
            commandBus.dispatch<CompleteBookingCommand, Booking>(
                CompleteBookingCommand(
                    bookingId = bookingId,
                    correlationId = correlationId,
                )
            )
        }

        coVerify {
            commandBus.dispatch<BookTravelOfferCommand, TravelOffer>(
                BookTravelOfferCommand(
                    travelOfferId = travelOfferId,
                    correlationId = correlationId,
                    bookingId = bookingId,
                    seat = seat,
                )
            )
        }
    }

    @Test
    fun `should handle BookingSagaFailedEvent`() = runTest(UnconfinedTestDispatcher()) {
        BookingSagaEventHandler(eventBus, commandBus, backgroundScope)

        val bookingId = BookingId.generate()
        val correlationId = UUID.randomUUID()
        val message = "Error message"

        eventBus.publish(EventEnvelope(
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
            commandBus.dispatch<FailBookingCommand, Booking>(
                FailBookingCommand(
                    bookingId = bookingId,
                    correlationId = correlationId,
                    message = message,
                )
            )
        }
    }
}
