package pl.szymanski.wiktor.ta.eventhandler

import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
import pl.szymanski.wiktor.ta.domain.event.AttractionAvailableEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionFullEvent
import pl.szymanski.wiktor.ta.service.TravelOfferService
import java.util.UUID
import kotlin.test.Test

class AttractionEventHandleLogicTest {
    private val travelOfferService = mockk<TravelOfferService>(relaxed = true)
    private val logic = AttractionEventHandleLogic(travelOfferService)

    @Test
    fun `should handle AttractionExpiredEvent`() = runTest {
        val attractionId = AttractionId.generate() as AttractionId.Present
        val correlationId = UUID.randomUUID()

        logic.onExpiredEvent(EventEnvelope(
            AttractionExpiredEvent(
                attractionId = attractionId,
            ),
            Metadata(
                correlationId,
                0
            )
        ))

        coVerify { travelOfferService.expireTravelOffer(attractionId, correlationId) }
    }

    @Test
    fun `should handle AttractionFullEvent`() = runTest {
        val attractionId = AttractionId.generate() as AttractionId.Present
        val correlationId = UUID.randomUUID()

        logic.onBookedEvent(EventEnvelope(
            AttractionFullEvent(
                attractionId = attractionId,
            ),
            Metadata(
                correlationId,
                0
            )
        ))

        coVerify { travelOfferService.makeTravelOfferUnavailable(attractionId, correlationId) }
    }

    @Test
    fun `should handle AttractionAvailableEvent`() = runTest {
        val attractionId = AttractionId.generate() as AttractionId.Present
        val correlationId = UUID.randomUUID()

        logic.onBookingCanceledEvent(EventEnvelope(
            AttractionAvailableEvent(
                attractionId = attractionId,
            ),
            Metadata(
                correlationId,
                0
            )
        ))

        coVerify { travelOfferService.makeTravelOfferAvailable(attractionId, correlationId) }
    }
}
