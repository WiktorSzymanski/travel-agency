//package pl.szymanski.wiktor.ta.eventHandlerLogic
//
//import pl.szymanski.wiktor.ta.EventEnvelope
//import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
//import pl.szymanski.wiktor.ta.domain.event.AttractionAvailableEvent
//import pl.szymanski.wiktor.ta.domain.event.AttractionExpiredEvent
//import pl.szymanski.wiktor.ta.domain.event.AttractionFullEvent
//import pl.szymanski.wiktor.ta.service.TravelOfferService
//
//class AttractionEventHandleLogic(
//    private val travelOfferService: TravelOfferService
//) {
//    suspend fun onExpiredEvent(envelope: EventEnvelope<AttractionExpiredEvent>) =
//        travelOfferService.expireTravelOffer(envelope.event.attractionId as AttractionId.Present, envelope.metadata.correlationId)
//
//    suspend fun onBookedEvent(envelope: EventEnvelope<AttractionFullEvent>) =
//        travelOfferService.makeTravelOfferUnavailable(
//            envelope.event.attractionId as AttractionId.Present,
//            envelope.metadata.correlationId)
//
//    suspend fun onBookingCanceledEvent(envelope: EventEnvelope<AttractionAvailableEvent>) =
//        travelOfferService.makeTravelOfferAvailable(envelope.event.attractionId as AttractionId.Present, envelope.metadata.correlationId)
//}
