package pl.szymanski.wiktor.ta.infrastructure.function

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.EventGridTrigger
import com.microsoft.azure.functions.annotation.FunctionName
import kotlinx.coroutines.runBlocking
import pl.szymanski.wiktor.ta.command.ReserveTravelOfferCommand
import pl.szymanski.wiktor.ta.command.TravelOfferCommand
import pl.szymanski.wiktor.ta.domain.event.BookingCreatedEvent
import pl.szymanski.wiktor.ta.infrastructure.repository.EventJsonSerializer
import pl.szymanski.wiktor.ta.infrastructure.scheduler.PersistedEvent

@FunctionName("BookingCreatedEventHandler")
fun bookingCreatedEventHandler(
    @EventGridTrigger(name = "eventgrid") event: PersistedEvent,
    context: ExecutionContext
) {
    require(event.type == BookingCreatedEvent::class.java.name)
    val domainEvent =
        EventJsonSerializer.fromJSON(
            event.domainevent,
            Class.forName(event.type)
        ) as BookingCreatedEvent

    runBlocking {
        travelOfferCommandHandler.handle(
            ReserveTravelOfferCommand(
                domainEvent.travelOfferId,
                domainEvent.correlationId!!,
                domainEvent.bookingId,
                domainEvent.seat,
            ) as TravelOfferCommand,
        )
    }
    context.logger.info("Booked travel offer: ${domainEvent.travelOfferId} by user ${domainEvent.userId}")
}