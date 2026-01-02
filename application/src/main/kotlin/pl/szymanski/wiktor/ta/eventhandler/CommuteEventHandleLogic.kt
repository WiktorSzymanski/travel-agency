package pl.szymanski.wiktor.ta.eventhandler

import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.domain.event.CommuteAvailableEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteFullEvent
import pl.szymanski.wiktor.ta.service.TravelOfferService

class CommuteEventHandleLogic(
    private val travelOfferService: TravelOfferService
) {
    suspend fun onExpiredEvent(envelope: EventEnvelope<CommuteExpiredEvent>) =
        travelOfferService.expireTravelOffer(envelope.event.commuteId, envelope.metadata.correlationId)

    suspend fun onBookedEvent(envelope: EventEnvelope<CommuteFullEvent>) =
        travelOfferService.makeTravelOfferUnavailable(envelope.event.commuteId, envelope.metadata.correlationId)

    suspend fun onCanceledEvent(envelope: EventEnvelope<CommuteAvailableEvent>) =
        travelOfferService.makeTravelOfferAvailable(envelope.event.commuteId, envelope.metadata.correlationId)
}
