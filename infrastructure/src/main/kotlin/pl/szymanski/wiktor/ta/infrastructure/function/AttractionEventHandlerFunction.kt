package pl.szymanski.wiktor.ta.infrastructure.function

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.EventGridTrigger
import com.microsoft.azure.functions.annotation.FunctionName
import kotlinx.coroutines.runBlocking
import pl.szymanski.wiktor.ta.domain.event.AttractionAvailableEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionFullEvent
import pl.szymanski.wiktor.ta.infrastructure.repository.EventJsonSerializer
import pl.szymanski.wiktor.ta.infrastructure.scheduler.PersistedEvent
import pl.szymanski.wiktor.ta.service.TravelOfferStatusService
import pl.szymanski.wiktor.ta.infrastructure.repository.query.TravelOfferQueryRepositoryImpl
import pl.szymanski.wiktor.ta.infrastructure.repository.command.TravelOfferRepositoryImpl
import pl.szymanski.wiktor.ta.commandHandler.TravelOfferCommandHandler


private val travelOfferStatusServiceByAttraction = TravelOfferStatusService(
    travelOfferRepository = TravelOfferQueryRepositoryImpl(),
    travelOfferCommandHandler = TravelOfferCommandHandler(
        TravelOfferRepositoryImpl(),
        QueueObjectWrapper())
)

@FunctionName("AttractionFullEventHandler")
fun attractionFullEventHandler(
    @EventGridTrigger(name = "eventgrid") event: PersistedEvent,
    context: ExecutionContext
) {
    require(event.type == AttractionFullEvent::class.java.name)
    val domainEvent = EventJsonSerializer.fromJSON(event.domainevent, Class.forName(event.type)) as AttractionFullEvent

    runBlocking {
        travelOfferStatusServiceByAttraction.makeTravelOfferUnavailableByAttraction(domainEvent.attractionId, domainEvent.correlationId!!)
    }

    context.logger.info("Marked offers unavailable by attraction ${domainEvent.attractionId}")
}

@FunctionName("AttractionAvailableEventHandler")
fun attractionAvailableEventHandler(
    @EventGridTrigger(name = "eventgrid") event: PersistedEvent,
    context: ExecutionContext
) {
    require(event.type == AttractionAvailableEvent::class.java.name)
    val domainEvent = EventJsonSerializer.fromJSON(event.domainevent, Class.forName(event.type)) as AttractionAvailableEvent

    runBlocking {
        travelOfferStatusServiceByAttraction.makeTravelOfferAvailableByAttraction(domainEvent.attractionId, domainEvent.correlationId!!)
    }

    context.logger.info("Marked offers available by attraction ${domainEvent.attractionId}")
}
