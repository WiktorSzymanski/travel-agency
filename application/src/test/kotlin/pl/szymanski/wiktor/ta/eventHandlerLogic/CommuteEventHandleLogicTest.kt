package pl.szymanski.wiktor.ta.eventHandlerLogic

import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.domain.aggregate.CommuteId
import pl.szymanski.wiktor.ta.domain.event.CommuteAvailableEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteFullEvent
import pl.szymanski.wiktor.ta.service.TravelOfferService
import java.util.UUID
import kotlin.test.Test

class CommuteEventHandleLogicTest {
    private val travelOfferService = mockk<TravelOfferService>(relaxed = true)
    private val logic = CommuteEventHandleLogic(travelOfferService)

    @Test
    fun `should handle CommuteExpiredEvent`() = runTest {
        val commuteId = CommuteId.generate()
        val correlationId = UUID.randomUUID()

        logic.onExpiredEvent(EventEnvelope(
            CommuteExpiredEvent(
                commuteId = commuteId,
            ),
            Metadata(
                correlationId,
                0
            )
        ))

        coVerify { travelOfferService.expireTravelOffer(commuteId, correlationId) }
    }

    @Test
    fun `should handle CommuteFullEvent`() = runTest {
        val commuteId = CommuteId.generate()
        val correlationId = UUID.randomUUID()

        logic.onBookedEvent(EventEnvelope(
            CommuteFullEvent(
                commuteId = commuteId,
            ),
            Metadata(
                correlationId,
                0
            )
        ))

        coVerify { travelOfferService.makeTravelOfferUnavailable(commuteId, correlationId) }
    }

    @Test
    fun `should handle CommuteAvailableEvent`() = runTest {
        val commuteId = CommuteId.generate()
        val correlationId = UUID.randomUUID()

        logic.onCanceledEvent(EventEnvelope(
            CommuteAvailableEvent(
                commuteId = commuteId,
            ),
            Metadata(
                correlationId,
                0
            )
        ))

        coVerify { travelOfferService.makeTravelOfferAvailable(commuteId, correlationId) }
    }
}
