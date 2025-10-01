package pl.szymanski.wiktor.ta.infrastructure.function

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.EventGridTrigger
import com.microsoft.azure.functions.annotation.FunctionName
import kotlinx.coroutines.runBlocking
import pl.szymanski.wiktor.ta.domain.event.CommuteAvailableEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteFullEvent
import pl.szymanski.wiktor.ta.infrastructure.repository.EventJsonSerializer
import pl.szymanski.wiktor.ta.infrastructure.scheduler.PersistedEvent
import pl.szymanski.wiktor.ta.service.TravelOfferStatusService
import pl.szymanski.wiktor.ta.infrastructure.repository.query.TravelOfferQueryRepositoryImpl
import pl.szymanski.wiktor.ta.infrastructure.repository.command.TravelOfferRepositoryImpl
import pl.szymanski.wiktor.ta.commandHandler.TravelOfferCommandHandler

private val travelOfferStatusServiceByCommute = TravelOfferStatusService(
    travelOfferRepository = TravelOfferQueryRepositoryImpl(),
    travelOfferCommandHandler = TravelOfferCommandHandler(
        TravelOfferRepositoryImpl(),
        QueueObjectWrapper())
)

@FunctionName("CommuteFullEventHandler")
fun commuteFullEventHandler(
    @EventGridTrigger(name = "eventgrid") event: PersistedEvent,
    context: ExecutionContext
) {
    require(event.type == CommuteFullEvent::class.java.name)
    val domainEvent = EventJsonSerializer.fromJSON(event.domainevent, Class.forName(event.type)) as CommuteFullEvent

    runBlocking {
        travelOfferStatusServiceByCommute.makeTravelOfferUnavailableByCommute(domainEvent.commuteId, domainEvent.correlationId!!)
    }

    context.logger.info("Marked offers unavailable by commute ${domainEvent.commuteId}")
}

@FunctionName("CommuteAvailableEventHandler")
fun commuteAvailableEventHandler(
    @EventGridTrigger(name = "eventgrid") event: PersistedEvent,
    context: ExecutionContext
) {
    require(event.type == CommuteAvailableEvent::class.java.name)
    val domainEvent = EventJsonSerializer.fromJSON(event.domainevent, Class.forName(event.type)) as CommuteAvailableEvent

    runBlocking {
        travelOfferStatusServiceByCommute.makeTravelOfferAvailableByCommute(domainEvent.commuteId, domainEvent.correlationId!!)
    }

    context.logger.info("Marked offers available by commute ${domainEvent.commuteId}")
}
