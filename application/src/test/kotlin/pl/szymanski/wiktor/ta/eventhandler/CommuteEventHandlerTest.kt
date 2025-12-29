package pl.szymanski.wiktor.ta.eventhandler

import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.domain.event.CommuteAvailableEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteFullEvent
import pl.szymanski.wiktor.ta.saga.DummyEventBus
import pl.szymanski.wiktor.ta.service.TravelOfferService
import java.util.UUID
import kotlin.test.Test

class CommuteEventHandlerTest {
    private val eventBus = DummyEventBus()
    private val travelOfferService = mockk<TravelOfferService>(relaxed = true)

    @Test
    fun `should handle CommuteExpiredEvent`() = runTest(UnconfinedTestDispatcher()) {
        CommuteEventHandler(eventBus, travelOfferService, backgroundScope)

        val commuteId = UUID.randomUUID()
        val correlationId = UUID.randomUUID()

        eventBus.publish(EventEnvelope(
            CommuteExpiredEvent(
                commuteId = commuteId,
            ),
            Metadata(
                correlationId,
                0
            )
        ))

        coVerify { travelOfferService.expireTravelOfferByCommute(commuteId, correlationId) }
    }

    @Test
    fun `should handle CommuteFullEvent`() = runTest(UnconfinedTestDispatcher()) {
        CommuteEventHandler(eventBus, travelOfferService, backgroundScope)

        val commuteId = UUID.randomUUID()
        val correlationId = UUID.randomUUID()

        eventBus.publish(EventEnvelope(
            CommuteFullEvent(
                commuteId = commuteId,
            ),
            Metadata(
                correlationId,
                0
            )
        ))

        coVerify { travelOfferService.makeTravelOfferUnavailableByCommute(commuteId, correlationId) }
    }

    @Test
    fun `should handle CommuteAvailableEvent`() = runTest(UnconfinedTestDispatcher()) {
        CommuteEventHandler(eventBus, travelOfferService, backgroundScope)

        val commuteId = UUID.randomUUID()
        val correlationId = UUID.randomUUID()

        eventBus.publish(EventEnvelope(
            CommuteAvailableEvent(
                commuteId = commuteId,
            ),
            Metadata(
                correlationId,
                0
            )
        ))

        coVerify { travelOfferService.makeTravelOfferAvailableByCommute(commuteId, correlationId) }
    }
}
