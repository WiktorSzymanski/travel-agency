package pl.szymanski.wiktor.ta.eventhandler

import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationExpiredEvent
import pl.szymanski.wiktor.ta.saga.DummyEventBus
import pl.szymanski.wiktor.ta.service.TravelOfferService
import java.util.UUID
import kotlin.test.Test

class AccommodationEventHandlerTest {
    private val eventBus = DummyEventBus()
    private val travelOfferService = mockk<TravelOfferService>(relaxed = true)

    @Test
    fun `should handle AccommodationBookingCanceledEvent`() = runTest(UnconfinedTestDispatcher()) {
        AccommodationEventHandler(eventBus, travelOfferService, backgroundScope)

        val accommodationId = UUID.randomUUID()
        val bookingId = UUID.randomUUID()
        val correlationId = UUID.randomUUID()

        eventBus.publish(EventEnvelope(
            AccommodationBookingCanceledEvent(
                accommodationId = accommodationId,
                bookingId = bookingId,
            ),
            Metadata(
                correlationId,
                0
            )
        ))

        coVerify { travelOfferService.makeTravelOfferAvailableByAccommodation(accommodationId, correlationId) }
    }

    @Test
    fun `should handle AccommodationExpiredEvent`() = runTest(UnconfinedTestDispatcher()) {
        AccommodationEventHandler(eventBus, travelOfferService, backgroundScope)

        val accommodationId = UUID.randomUUID()
        val correlationId = UUID.randomUUID()

        eventBus.publish(EventEnvelope(
            AccommodationExpiredEvent(
                accommodationId = accommodationId,
            ),
            Metadata(
                correlationId,
                0
            )
        ))

        coVerify { travelOfferService.expireTravelOfferByAccommodation(accommodationId, correlationId) }
    }

    @Test
    fun `should handle AccommodationBookedEvent`() = runTest(UnconfinedTestDispatcher()) {
        AccommodationEventHandler(eventBus, travelOfferService, backgroundScope)

        val accommodationId = UUID.randomUUID()
        val bookingId = UUID.randomUUID()
        val correlationId = UUID.randomUUID()

        eventBus.publish(EventEnvelope(
            AccommodationBookedEvent(
                accommodationId = accommodationId,
                bookingId = bookingId,
            ),
            Metadata(
                correlationId,
                0
            )
        ))

        coVerify { travelOfferService.makeTravelOfferUnavailableByAccommodation(accommodationId, correlationId) }
    }
}