package pl.szymanski.wiktor.ta.eventhandler

import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import pl.szymanski.wiktor.ta.CommandBus
import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.aggregate.AccommodationId
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import pl.szymanski.wiktor.ta.domain.aggregate.CommuteId
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOfferId
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReleaseEvent
import pl.szymanski.wiktor.ta.service.TravelOfferService
import java.util.UUID
import kotlin.test.Test

class TravelOfferEventHandleLogicTest {
    private val eventBus = mockk<EventBus>(relaxed = true)
    private val commandBus = mockk<CommandBus>(relaxed = true)
    private val travelOfferService = mockk<TravelOfferService>(relaxed = true)
    private val logic = TravelOfferEventHandleLogic(eventBus, commandBus, travelOfferService)

    @Test
    fun `should handle TravelOfferReleaseEvent`() = runTest {
        val travelOfferId = TravelOfferId.from(UUID.randomUUID())
        val correlationId = UUID.randomUUID()

        logic.onReleaseEvent(EventEnvelope(
            TravelOfferReleaseEvent(
                travelOfferId = travelOfferId,
                accommodationId = AccommodationId.generate(),
                commuteId = CommuteId.generate(),
                attractionId = AttractionId.Empty,
                bookingId = BookingId.generate(),
                seat = Seat.Any
            ),
            Metadata(
                correlationId,
                0
            )
        ))
    }
}
