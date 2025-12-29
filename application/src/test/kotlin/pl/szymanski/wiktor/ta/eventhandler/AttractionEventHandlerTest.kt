package pl.szymanski.wiktor.ta.eventhandler

import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.domain.event.AttractionAvailableEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionFullEvent
import pl.szymanski.wiktor.ta.saga.DummyEventBus
import pl.szymanski.wiktor.ta.service.TravelOfferService
import java.util.UUID
import kotlin.test.Test

class AttractionEventHandlerTest {
    private val eventBus = DummyEventBus()
    private val travelOfferService = mockk<TravelOfferService>(relaxed = true)

    @Test
    fun `should handle AttractionExpiredEvent`() = runTest(UnconfinedTestDispatcher()) {
        AttractionEventHandler(eventBus, travelOfferService, backgroundScope)

        val attractionId = UUID.randomUUID()
        val correlationId = UUID.randomUUID()

        eventBus.publish(EventEnvelope(
            AttractionExpiredEvent(
                attractionId = attractionId,
            ),
            Metadata(
                correlationId,
                0
            )
        ))

        coVerify { travelOfferService.expireTravelOfferByAttraction(attractionId, correlationId) }
    }

    @Test
    fun `should handle AttractionFullEvent`() = runTest(UnconfinedTestDispatcher()) {
        AttractionEventHandler(eventBus, travelOfferService, backgroundScope)

        val attractionId = UUID.randomUUID()
        val correlationId = UUID.randomUUID()

        eventBus.publish(EventEnvelope(
            AttractionFullEvent(
                attractionId = attractionId,
            ),
            Metadata(
                correlationId,
                0
            )
        ))

        coVerify { travelOfferService.makeTravelOfferUnavailableByAttraction(attractionId, correlationId) }
    }

    @Test
    fun `should handle AttractionAvailableEvent`() = runTest(UnconfinedTestDispatcher()) {
        AttractionEventHandler(eventBus, travelOfferService, backgroundScope)

        val attractionId = UUID.randomUUID()
        val correlationId = UUID.randomUUID()

        eventBus.publish(EventEnvelope(
            AttractionAvailableEvent(
                attractionId = attractionId,
            ),
            Metadata(
                correlationId,
                0
            )
        ))

        coVerify { travelOfferService.makeTravelOfferAvailableByAttraction(attractionId, correlationId) }
    }
}
