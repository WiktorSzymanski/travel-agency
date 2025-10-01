package pl.szymanski.wiktor.ta.infrastructure.function

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.EventGridTrigger
import com.microsoft.azure.functions.annotation.FunctionName
import kotlinx.coroutines.runBlocking
import pl.szymanski.wiktor.ta.infrastructure.repository.EventJsonSerializer
import pl.szymanski.wiktor.ta.infrastructure.scheduler.PersistedEvent
import pl.szymanski.wiktor.ta.saga.BookingSaga
import pl.szymanski.wiktor.ta.saga.CancelBookingSaga
import pl.szymanski.wiktor.ta.service.TravelOfferStatusService
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReservedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReleaseEvent
import pl.szymanski.wiktor.ta.infrastructure.repository.command.TravelOfferRepositoryImpl
import pl.szymanski.wiktor.ta.infrastructure.repository.query.TravelOfferQueryRepositoryImpl
import pl.szymanski.wiktor.ta.commandHandler.TravelOfferCommandHandler

// Local status service for saga compensation checks
private val travelOfferStatusServiceForSaga = TravelOfferStatusService(
    travelOfferRepository = TravelOfferQueryRepositoryImpl(),
    travelOfferCommandHandler = TravelOfferCommandHandler(
        TravelOfferRepositoryImpl(),
        QueueObjectWrapper())
)

@FunctionName("TravelOfferReservedEventHandler")
fun travelOfferReservedEventHandler(
    @EventGridTrigger(name = "eventgrid") event: PersistedEvent,
    context: ExecutionContext
) {
    require(event.type == TravelOfferReservedEvent::class.java.name)
    val domainEvent = EventJsonSerializer.fromJSON(event.domainevent, Class.forName(event.type)) as TravelOfferReservedEvent

    runBlocking {
        BookingSaga(
            travelOfferCommandHandler,
            attractionCommandHandler,
            commuteCommandHandler,
            accommodationCommandHandler,
            travelOfferStatusServiceForSaga,
            domainEvent
        ).execute()
    }

    context.logger.info("Started BookingSaga for reserved travel offer: ${domainEvent.travelOfferId}")
}

@FunctionName("TravelOfferReleaseEventHandler")
fun travelOfferReleaseEventHandler(
    @EventGridTrigger(name = "eventgrid") event: PersistedEvent,
    context: ExecutionContext
) {
    require(event.type == TravelOfferReleaseEvent::class.java.name)
    val domainEvent = EventJsonSerializer.fromJSON(event.domainevent, Class.forName(event.type)) as TravelOfferReleaseEvent

    runBlocking {
        CancelBookingSaga(
            travelOfferCommandHandler,
            attractionCommandHandler,
            commuteCommandHandler,
            accommodationCommandHandler,
            travelOfferStatusServiceForSaga,
            domainEvent
        ).execute()
    }

    context.logger.info("Started CancelBookingSaga for travel offer release: ${domainEvent.travelOfferId}")
}
