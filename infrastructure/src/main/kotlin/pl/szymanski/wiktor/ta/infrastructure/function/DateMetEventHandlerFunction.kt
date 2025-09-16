package pl.szymanski.wiktor.ta.infrastructure.function

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.EventGridTrigger
import com.microsoft.azure.functions.annotation.FunctionName
import kotlinx.coroutines.runBlocking
import pl.szymanski.wiktor.ta.command.AccommodationCommand
import pl.szymanski.wiktor.ta.command.AttractionCommand
import pl.szymanski.wiktor.ta.command.CommuteCommand
import pl.szymanski.wiktor.ta.command.ExpireAccommodationCommand
import pl.szymanski.wiktor.ta.command.ExpireAttractionCommand
import pl.szymanski.wiktor.ta.command.ExpireCommuteCommand
import pl.szymanski.wiktor.ta.event.AccommodationDateMetEvent
import pl.szymanski.wiktor.ta.event.AttractionDateMetEvent
import pl.szymanski.wiktor.ta.event.CommuteDateMetEvent
import pl.szymanski.wiktor.ta.infrastructure.repository.EventJsonSerializer
import pl.szymanski.wiktor.ta.infrastructure.scheduler.PersistedEvent

@FunctionName("AccommodationDateMetEventHandler")
fun accommodationDateMetEventHandler(
    @EventGridTrigger(name = "eventgrid") event: PersistedEvent,
    context: ExecutionContext
) {
    require(event.type == AccommodationDateMetEvent::class.java.name)
    val domainEvent = EventJsonSerializer.fromJSON(event.domainevent, Class.forName(event.type)) as AccommodationDateMetEvent

    runBlocking {
        accommodationCommandHandler.handle(
            ExpireAccommodationCommand(
                accommodationId = domainEvent.accommodationId,
                correlationId = domainEvent.correlationId!!,
            ) as AccommodationCommand,
        )
    }
    context.logger.info("Accommodation date met: ${domainEvent.accommodationId}")
}

@FunctionName("AttractionDateMetEventHandler")
fun attractionDateMetEventHandler(
    @EventGridTrigger(name = "eventgrid") event: PersistedEvent,
    context: ExecutionContext
) {
    require(event.type == AttractionDateMetEvent::class.java.name)
    val domainEvent = EventJsonSerializer.fromJSON(event.domainevent, Class.forName(event.type)) as AttractionDateMetEvent

    runBlocking {
        attractionCommandHandler.handle(
            ExpireAttractionCommand(
                attractionId = domainEvent.attractionId,
                correlationId = domainEvent.correlationId!!,
            ) as AttractionCommand,
        )
    }
    context.logger.info("Attraction date met: ${domainEvent.attractionId}")
}

@FunctionName("CommuteDateMetEventHandler")
fun commuteDateMetEventHandler(
    @EventGridTrigger(name = "eventgrid") event: PersistedEvent,
    context: ExecutionContext
) {
    require(event.type == CommuteDateMetEvent::class.java.name)
    val domainEvent = EventJsonSerializer.fromJSON(event.domainevent, Class.forName(event.type)) as CommuteDateMetEvent

    runBlocking {
        commuteCommandHandler.handle(
            ExpireCommuteCommand(
                commuteId = domainEvent.commuteId,
                correlationId = domainEvent.correlationId!!,
            ) as CommuteCommand,
        )
    }
    context.logger.info("Commute date met: ${domainEvent.commuteId}")
}