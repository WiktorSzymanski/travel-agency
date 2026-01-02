package pl.szymanski.wiktor.ta.eventhandler

import pl.szymanski.wiktor.ta.CommandBus
import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReleaseEvent
import pl.szymanski.wiktor.ta.saga.CancelBookingSaga
import pl.szymanski.wiktor.ta.service.TravelOfferService

class TravelOfferEventHandleLogic(
    private val eventBus: EventBus,
    private val commandBus: CommandBus,
    private val travelOfferService: TravelOfferService,
) {
    // TODO: maybe some singleton SAGA service? (GENERALLY saga needs revamp where it's state is stored in database)
    suspend fun onReleaseEvent(envelope: EventEnvelope<TravelOfferReleaseEvent>) =
        CancelBookingSaga(
            eventBus,
            commandBus,
            travelOfferService,
            envelope.event,
            envelope.metadata
        ).execute()
}
