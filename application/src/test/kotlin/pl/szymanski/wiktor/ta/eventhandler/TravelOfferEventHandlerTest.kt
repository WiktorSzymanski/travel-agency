package pl.szymanski.wiktor.ta.eventhandler

import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import pl.szymanski.wiktor.ta.CommandBus
import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReleaseEvent
import pl.szymanski.wiktor.ta.event.BookingCancelSagaStartedEvent
import pl.szymanski.wiktor.ta.saga.DummyEventBus
import pl.szymanski.wiktor.ta.service.TravelOfferService
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertTrue

class TravelOfferEventHandlerTest {
    private val eventBus = DummyEventBus()
    private val commandBus = mockk<CommandBus>(relaxed = true)
    private val travelOfferService = mockk<TravelOfferService>(relaxed = true)

    @Test
    fun `should handle TravelOfferReleaseEvent`() = runTest(UnconfinedTestDispatcher()) {
        TravelOfferEventHandler(eventBus, commandBus, travelOfferService, backgroundScope)

        val travelOfferId = UUID.randomUUID()
        val accommodationId = UUID.randomUUID()
        val commuteId = UUID.randomUUID()
        val attractionId = UUID.randomUUID()
        val bookingId = UUID.randomUUID()
        val correlationId = UUID.randomUUID()
        val seat = Seat.Any

        eventBus.publish(EventEnvelope(
            TravelOfferReleaseEvent(
                travelOfferId = travelOfferId,
                accommodationId = accommodationId,
                commuteId = commuteId,
                attractionId = attractionId,
                bookingId = bookingId,
                seat = seat,
            ),
            Metadata(
                correlationId,
                0
            )
        ))

        val publishedEvents = eventBus.emittedEvents.map { it.event }
        assertTrue(publishedEvents.any { it is BookingCancelSagaStartedEvent && it.bookingId == bookingId })
    }
}
