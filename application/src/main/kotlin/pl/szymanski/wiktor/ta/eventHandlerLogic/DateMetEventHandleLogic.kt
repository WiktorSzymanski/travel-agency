package pl.szymanski.wiktor.ta.eventHandlerLogic

import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.commands.accommodation.expire.ExpireAccommodationCommand
import pl.szymanski.wiktor.ta.commands.accommodation.expire.ExpireAccommodationCommandHandler
import pl.szymanski.wiktor.ta.commands.attraction.expire.ExpireAttractionCommand
import pl.szymanski.wiktor.ta.commands.attraction.expire.ExpireAttractionCommandHandler
import pl.szymanski.wiktor.ta.commands.commute.expire.ExpireCommuteCommand
import pl.szymanski.wiktor.ta.commands.commute.expire.ExpireCommuteCommandHandler
import pl.szymanski.wiktor.ta.event.AccommodationDateMetEvent
import pl.szymanski.wiktor.ta.event.AttractionDateMetEvent
import pl.szymanski.wiktor.ta.event.CommuteDateMetEvent

suspend fun onCommuteDateMetEvent(
    expireCommuteCommandHandler: ExpireCommuteCommandHandler,
    envelope: EventEnvelope<CommuteDateMetEvent>
) = expireCommuteCommandHandler.handle(
    ExpireCommuteCommand(
        envelope.metadata.correlationId,
        envelope.event.commuteId,
    )
)

suspend fun onAccommodationDateMetEvent(
    expireAccommodationCommandHandler: ExpireAccommodationCommandHandler,
    envelope: EventEnvelope<AccommodationDateMetEvent>
) = expireAccommodationCommandHandler.handle(
    ExpireAccommodationCommand(
        accommodationId = envelope.event.accommodationId,
        correlationId = envelope.metadata.correlationId,
    )
)

suspend fun onAttractionDateMetEvent(
    expireAttractionCommandHandler: ExpireAttractionCommandHandler,
    envelope: EventEnvelope<AttractionDateMetEvent>
) = expireAttractionCommandHandler.handle(
    ExpireAttractionCommand(
        attractionId = envelope.event.attractionId,
        correlationId = envelope.metadata.correlationId,
    )
)
