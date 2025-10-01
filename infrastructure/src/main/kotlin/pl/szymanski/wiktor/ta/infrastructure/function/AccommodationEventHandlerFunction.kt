package pl.szymanski.wiktor.ta.infrastructure.function

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.EventGridTrigger
import com.microsoft.azure.functions.annotation.FunctionName
import kotlinx.coroutines.runBlocking
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookingCanceledEvent
import pl.szymanski.wiktor.ta.infrastructure.repository.EventJsonSerializer
import pl.szymanski.wiktor.ta.infrastructure.scheduler.PersistedEvent
import pl.szymanski.wiktor.ta.service.TravelOfferStatusService
import pl.szymanski.wiktor.ta.infrastructure.repository.query.TravelOfferQueryRepositoryImpl
import pl.szymanski.wiktor.ta.infrastructure.repository.command.TravelOfferRepositoryImpl
import pl.szymanski.wiktor.ta.commandHandler.TravelOfferCommandHandler

private val travelOfferStatusServiceByAccommodation = TravelOfferStatusService(
    travelOfferRepository = TravelOfferQueryRepositoryImpl(),
    travelOfferCommandHandler = TravelOfferCommandHandler(
        TravelOfferRepositoryImpl(),
        QueueObjectWrapper())
)

@FunctionName("AccommodationBookedEventHandler")
fun accommodationBookedEventHandler(
    @EventGridTrigger(name = "eventgrid") event: PersistedEvent,
    context: ExecutionContext
) {
    require(event.type == AccommodationBookedEvent::class.java.name)
    val domainEvent = EventJsonSerializer.fromJSON(event.domainevent, Class.forName(event.type)) as AccommodationBookedEvent

    runBlocking {
        travelOfferStatusServiceByAccommodation.makeTravelOfferUnavailableByAccommodation(domainEvent.accommodationId, domainEvent.correlationId!!)
    }

    context.logger.info("Marked offers unavailable by accommodation ${domainEvent.accommodationId}")
}

@FunctionName("AccommodationBookingCanceledEventHandler")
fun accommodationBookingCanceledEventHandler(
    @EventGridTrigger(name = "eventgrid") event: PersistedEvent,
    context: ExecutionContext
) {
    require(event.type == AccommodationBookingCanceledEvent::class.java.name)
    val domainEvent = EventJsonSerializer.fromJSON(event.domainevent, Class.forName(event.type)) as AccommodationBookingCanceledEvent

    runBlocking {
        travelOfferStatusServiceByAccommodation.makeTravelOfferAvailableByAccommodation(domainEvent.accommodationId, domainEvent.correlationId!!)
    }

    context.logger.info("Marked offers available by accommodation ${domainEvent.accommodationId}")
}
