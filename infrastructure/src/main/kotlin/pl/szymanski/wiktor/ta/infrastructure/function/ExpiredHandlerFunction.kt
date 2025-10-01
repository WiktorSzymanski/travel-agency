package pl.szymanski.wiktor.ta.infrastructure.function

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.EventGridTrigger
import com.microsoft.azure.functions.annotation.FunctionName
import kotlinx.coroutines.runBlocking
import pl.szymanski.wiktor.ta.commandHandler.TravelOfferCommandHandler
import pl.szymanski.wiktor.ta.domain.event.AccommodationExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteExpiredEvent
import pl.szymanski.wiktor.ta.infrastructure.repository.EventJsonSerializer
import pl.szymanski.wiktor.ta.infrastructure.repository.command.TravelOfferRepositoryImpl
import pl.szymanski.wiktor.ta.infrastructure.repository.query.TravelOfferQueryRepositoryImpl
import pl.szymanski.wiktor.ta.infrastructure.scheduler.PersistedEvent
import pl.szymanski.wiktor.ta.service.TravelOfferExpireService

val travelOfferExpireService = TravelOfferExpireService(
    travelOfferRepository = TravelOfferQueryRepositoryImpl(),
    travelOfferCommandHandler = TravelOfferCommandHandler(
        TravelOfferRepositoryImpl(),
        QueueObjectWrapper())
)

@FunctionName("AccommodationExpiredEventHandler")
fun accommodationExpiredEventHandler(
    @EventGridTrigger(name = "eventgrid") event: PersistedEvent,
    context: ExecutionContext
) {
    require(event.type == AccommodationExpiredEvent::class.java.name)
    val domainEvent =
        EventJsonSerializer.fromJSON(
            event.domainevent,
            Class.forName(event.type)
        ) as AccommodationExpiredEvent

    runBlocking {
        travelOfferExpireService.expireTravelOfferByAccommodation(
            domainEvent.accommodationId,
            domainEvent.correlationId!!)
    }
    context.logger.info("Travel Offers with Accommodation ${domainEvent.accommodationId} should be expired")
}

@FunctionName("AttractionExpiredEventHandler")
fun attractionExpiredEventHandler(
    @EventGridTrigger(name = "eventgrid") event: PersistedEvent,
    context: ExecutionContext
) {
    require(event.type == AttractionExpiredEvent::class.java.name)
    val domainEvent =
        EventJsonSerializer.fromJSON(
            event.domainevent,
            Class.forName(event.type)
        ) as AttractionExpiredEvent

    runBlocking {
        travelOfferExpireService.expireTravelOfferByAttraction(
            domainEvent.attractionId,
            domainEvent.correlationId!!)
    }
    context.logger.info("Travel Offers with Attraction ${domainEvent.attractionId} should be expired")
}

@FunctionName("CommuteExpiredEventHandler")
fun commuteExpiredEventHandler(
    @EventGridTrigger(name = "eventgrid") event: PersistedEvent,
    context: ExecutionContext
) {
    require(event.type == CommuteExpiredEvent::class.java.name)
    val domainEvent =
        EventJsonSerializer.fromJSON(
            event.domainevent,
            Class.forName(event.type)
        ) as CommuteExpiredEvent

    runBlocking {
        travelOfferExpireService.expireTravelOfferByCommute(
            domainEvent.commuteId,
            domainEvent.correlationId!!)
    }
    context.logger.info("Travel Offers with Commute ${domainEvent.commuteId} should be expired")
}