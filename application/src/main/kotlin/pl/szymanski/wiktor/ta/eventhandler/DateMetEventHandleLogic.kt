package pl.szymanski.wiktor.ta.eventhandler

import pl.szymanski.wiktor.ta.CommandBus
import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.command.ExpireAccommodationCommand
import pl.szymanski.wiktor.ta.command.ExpireAttractionCommand
import pl.szymanski.wiktor.ta.command.ExpireCommuteCommand
import pl.szymanski.wiktor.ta.commandhandler.AccommodationCommandHandler
import pl.szymanski.wiktor.ta.commandhandler.AttractionCommandHandler
import pl.szymanski.wiktor.ta.commandhandler.CommuteCommandHandler
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.event.AccommodationDateMetEvent
import pl.szymanski.wiktor.ta.event.AttractionDateMetEvent
import pl.szymanski.wiktor.ta.event.CommuteDateMetEvent

class DateMetEventHandleLogic(
    private val commandBus: CommandBus
) {
    suspend fun onCommuteDateMetEvent(envelope: EventEnvelope<CommuteDateMetEvent>) =
        commandBus.dispatch<ExpireCommuteCommand, Commute>(
            ExpireCommuteCommand(
                envelope.event.commuteId,
                envelope.metadata.correlationId
            )
        )

    suspend fun onAccommodationDateMetEvent(envelope: EventEnvelope<AccommodationDateMetEvent>) =
        commandBus.dispatch<ExpireAccommodationCommand, Accommodation>(
            ExpireAccommodationCommand(
                accommodationId = envelope.event.accommodationId,
                correlationId = envelope.metadata.correlationId,
            )
        )


    suspend fun onAttractionDateMetEvent(envelope: EventEnvelope<AttractionDateMetEvent>) =
        commandBus.dispatch<ExpireAttractionCommand, Attraction>(
            ExpireAttractionCommand(
                attractionId = envelope.event.attractionId,
                correlationId = envelope.metadata.correlationId,
            )
        )
}
