package pl.szymanski.wiktor.ta.eventhandler

import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import pl.szymanski.wiktor.ta.CommandBus
import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.command.ReleaseTravelOfferCommand
import pl.szymanski.wiktor.ta.command.ReserveTravelOfferCommand
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOfferId
import pl.szymanski.wiktor.ta.domain.event.BookingCancelRequestedEvent
import pl.szymanski.wiktor.ta.domain.event.BookingCreatedEvent
import pl.szymanski.wiktor.ta.saga.DummyEventBus
import java.util.UUID
import kotlin.test.Test

class BookingEventHandlerTest {
    private val eventBus = DummyEventBus()
    private val commandBus = mockk<CommandBus>(relaxed = true)

    @Test
    fun `should handle BookingCreatedEvent`() = runTest(UnconfinedTestDispatcher()) {
        BookingEventHandler(eventBus, commandBus, backgroundScope)

        val travelOfferId = TravelOfferId.from(UUID.randomUUID())
        val bookingId = BookingId.generate()
        val userId = UUID.randomUUID()
        val correlationId = UUID.randomUUID()
        val seat = Seat.Any

        eventBus.publish(EventEnvelope(
            BookingCreatedEvent(
                travelOfferId = travelOfferId,
                bookingId = bookingId,
                userId = userId,
                seat = seat,
            ),
            Metadata(
                correlationId,
                0
            )
        ))

        coVerify {
            commandBus.dispatch<ReserveTravelOfferCommand, TravelOffer>(
                ReserveTravelOfferCommand(
                    travelOfferId,
                    correlationId,
                    bookingId,
                    seat,
                )
            )
        }
    }

    @Test
    fun `should handle BookingCancelRequestedEvent`() = runTest(UnconfinedTestDispatcher()) {
        BookingEventHandler(eventBus, commandBus, backgroundScope)

        val travelOfferId = TravelOfferId.from(UUID.randomUUID())
        val bookingId = BookingId.generate()
        val correlationId = UUID.randomUUID()
        val seat = Seat.Any

        eventBus.publish(EventEnvelope(
            BookingCancelRequestedEvent(
                travelOfferId = travelOfferId,
                bookingId = bookingId,
                seat = seat,
            ),
            Metadata(
                correlationId,
                0
            )
        ))

        coVerify {
            commandBus.dispatch<ReleaseTravelOfferCommand, TravelOffer>(
                ReleaseTravelOfferCommand(
                    travelOfferId,
                    correlationId,
                    bookingId,
                    seat,
                )
            )
        }
    }
}
