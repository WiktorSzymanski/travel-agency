package pl.szymanski.wiktor.ta.eventHandlerLogic

import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.commands.accommodation.expire.ExpireAccommodationCommand
import pl.szymanski.wiktor.ta.commands.accommodation.expire.ExpireAccommodationCommandHandler
import pl.szymanski.wiktor.ta.commands.attraction.expire.ExpireAttractionCommand
import pl.szymanski.wiktor.ta.commands.attraction.expire.ExpireAttractionCommandHandler
import pl.szymanski.wiktor.ta.commands.commute.expire.ExpireCommuteCommand
import pl.szymanski.wiktor.ta.commands.commute.expire.ExpireCommuteCommandHandler
import pl.szymanski.wiktor.ta.domain.aggregate.AccommodationId
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
import pl.szymanski.wiktor.ta.domain.aggregate.CommuteId
import pl.szymanski.wiktor.ta.event.AccommodationDateMetEvent
import pl.szymanski.wiktor.ta.event.AttractionDateMetEvent
import pl.szymanski.wiktor.ta.event.CommuteDateMetEvent

suspend fun onCommuteDateMetEvent(
    expireCommuteCommandHandler: ExpireCommuteCommandHandler,
    envelope: EventEnvelope<CommuteDateMetEvent>
) = expireCommuteCommandHandler.handle(
    ExpireCommuteCommand(
        envelope.metadata.correlationId,
        CommuteId.from(envelope.event.commuteId),
    )
)

suspend fun onAccommodationDateMetEvent(
    expireAccommodationCommandHandler: ExpireAccommodationCommandHandler,
    envelope: EventEnvelope<AccommodationDateMetEvent>
) = expireAccommodationCommandHandler.handle(
    ExpireAccommodationCommand(
        accommodationId = AccommodationId.from(envelope.event.accommodationId),
        correlationId = envelope.metadata.correlationId,
    )
)

suspend fun onAttractionDateMetEvent(
    expireAttractionCommandHandler: ExpireAttractionCommandHandler,
    envelope: EventEnvelope<AttractionDateMetEvent>
) = expireAttractionCommandHandler.handle(
    ExpireAttractionCommand(
        attractionId = AttractionId.from(envelope.event.attractionId),
        correlationId = envelope.metadata.correlationId,
    )
)
