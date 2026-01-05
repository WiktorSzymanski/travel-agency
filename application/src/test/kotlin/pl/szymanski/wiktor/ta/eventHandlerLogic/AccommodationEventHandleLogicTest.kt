package pl.szymanski.wiktor.ta.eventHandlerLogic

import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.domain.aggregate.AccommodationId
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationExpiredEvent
import pl.szymanski.wiktor.ta.service.TravelOfferService
import java.util.UUID
import kotlin.test.Test

class AccommodationEventHandleLogicTest {
    private val travelOfferService = mockk<TravelOfferService>(relaxed = true)
    private val logic = AccommodationEventHandleLogic(travelOfferService)

    @Test
    fun `should handle AccommodationBookingCanceledEvent`() = runTest {
        val accommodationId = AccommodationId.generate()
        val bookingId = BookingId.generate()
        val correlationId = UUID.randomUUID()

        logic.onCanceledEvent(EventEnvelope(
            AccommodationBookingCanceledEvent(
                accommodationId = accommodationId,
                bookingId = bookingId,
            ),
            Metadata(
                correlationId,
                0
            )
        ))

        coVerify { travelOfferService.makeTravelOfferAvailable(accommodationId, correlationId) }
    }

    @Test
    fun `should handle AccommodationExpiredEvent`() = runTest {
        val accommodationId = AccommodationId.generate()
        val correlationId = UUID.randomUUID()

        logic.onExpiredEvent(EventEnvelope(
            AccommodationExpiredEvent(
                accommodationId = accommodationId,
            ),
            Metadata(
                correlationId,
                0
            )
        ))

        coVerify { travelOfferService.expireTravelOffer(accommodationId, correlationId) }
    }

    @Test
    fun `should handle AccommodationBookedEvent`() = runTest {
        val accommodationId = AccommodationId.generate()
        val correlationId = UUID.randomUUID()

        logic.onBookedEvent(EventEnvelope(
            AccommodationExpiredEvent(
                accommodationId = accommodationId,
            ),
            Metadata(
                correlationId,
                0
            )
        ))

        coVerify { travelOfferService.makeTravelOfferUnavailable(accommodationId, correlationId) }
    }
}
