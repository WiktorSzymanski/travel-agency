package pl.szymanski.wiktor.ta.eventHandlerLogic

import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationExpiredEvent
import pl.szymanski.wiktor.ta.service.TravelOfferService

class AccommodationEventHandleLogic(
    private val travelOfferService: TravelOfferService
) {
    suspend fun onExpiredEvent(envelope: EventEnvelope<AccommodationExpiredEvent>) =
        travelOfferService.expireTravelOffer(envelope.event.accommodationId, envelope.metadata.correlationId)

    suspend fun onBookedEvent(envelope: EventEnvelope<AccommodationExpiredEvent>) =
        travelOfferService.makeTravelOfferUnavailable(envelope.event.accommodationId, envelope.metadata.correlationId)

    suspend fun onCanceledEvent(envelope: EventEnvelope<AccommodationBookingCanceledEvent>) =
        travelOfferService.makeTravelOfferAvailable(envelope.event.accommodationId, envelope.metadata.correlationId)
}
