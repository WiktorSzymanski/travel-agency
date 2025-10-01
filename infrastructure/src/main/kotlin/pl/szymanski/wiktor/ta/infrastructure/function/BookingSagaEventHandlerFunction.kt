package pl.szymanski.wiktor.ta.infrastructure.function

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.EventGridTrigger
import com.microsoft.azure.functions.annotation.FunctionName
import kotlinx.coroutines.runBlocking
import pl.szymanski.wiktor.ta.command.BookingCommand
import pl.szymanski.wiktor.ta.command.CompleteBookingCommand
import pl.szymanski.wiktor.ta.command.ProcessBookingCommand
import pl.szymanski.wiktor.ta.command.FailBookingCommand
import pl.szymanski.wiktor.ta.command.BookTravelOfferCommand
import pl.szymanski.wiktor.ta.command.TravelOfferCommand
import pl.szymanski.wiktor.ta.event.BookingSagaStartedEvent
import pl.szymanski.wiktor.ta.event.BookingSagaCompletedEvent
import pl.szymanski.wiktor.ta.event.BookingSagaFailedEvent
import pl.szymanski.wiktor.ta.infrastructure.repository.EventJsonSerializer
import pl.szymanski.wiktor.ta.infrastructure.scheduler.PersistedEvent

@FunctionName("BookingSagaStartedEventHandler")
fun bookingSagaStartedEventHandler(
    @EventGridTrigger(name = "eventgrid") event: PersistedEvent,
    context: ExecutionContext
) {
    context.logger.info("Handle BookingSagaStartedEvent")
    require(event.type == BookingSagaStartedEvent::class.java.name)
    val domainEvent = EventJsonSerializer.fromJSON(event.domainevent, Class.forName(event.type)) as BookingSagaStartedEvent

    runBlocking {
        bookingCommandHandler.handle(
            ProcessBookingCommand(
                bookingId = domainEvent.bookingId,
                correlationId = domainEvent.correlationId!!,
            ) as BookingCommand
        )
    }

    context.logger.info("Booking ${domainEvent.bookingId} in status PROCESS")
}

@FunctionName("BookingSagaCompletedEventHandler")
fun bookingSagaCompletedEventHandler(
    @EventGridTrigger(name = "eventgrid") event: PersistedEvent,
    context: ExecutionContext
) {
    require(event.type == BookingSagaCompletedEvent::class.java.name)
    val domainEvent = EventJsonSerializer.fromJSON(event.domainevent, Class.forName(event.type)) as BookingSagaCompletedEvent

    runBlocking {
        bookingCommandHandler.handle(
            CompleteBookingCommand(
                bookingId = domainEvent.bookingId,
                correlationId = domainEvent.correlationId!!,
            ) as BookingCommand
        )
    }

    context.logger.info("Completed booking ${domainEvent.bookingId}")
}

@FunctionName("BookingSagaCompletedEventHandler2")
fun bookingSagaCompletedEventHandler2(
    @EventGridTrigger(name = "eventgrid") event: PersistedEvent,
    context: ExecutionContext
) {
    require(event.type == BookingSagaCompletedEvent::class.java.name)
    val domainEvent = EventJsonSerializer.fromJSON(event.domainevent, Class.forName(event.type)) as BookingSagaCompletedEvent

    runBlocking {
        travelOfferCommandHandler.handle(
            BookTravelOfferCommand(
                travelOfferId = domainEvent.travelOfferId,
                correlationId = domainEvent.correlationId!!,
                bookingId = domainEvent.bookingId,
                seat = domainEvent.seat
            ) as TravelOfferCommand
        )
    }

    context.logger.info("Booked travel offer ${domainEvent.travelOfferId} after saga completion")
}

@FunctionName("BookingSagaFailedEventHandler")
fun bookingSagaFailedEventHandler(
    @EventGridTrigger(name = "eventgrid") event: PersistedEvent,
    context: ExecutionContext
) {
    require(event.type == BookingSagaFailedEvent::class.java.name)
    val domainEvent = EventJsonSerializer.fromJSON(event.domainevent, Class.forName(event.type)) as BookingSagaFailedEvent

    runBlocking {
        bookingCommandHandler.handle(
            FailBookingCommand(
                bookingId = domainEvent.bookingId,
                correlationId = domainEvent.correlationId!!,
                message = domainEvent.message
            ) as BookingCommand
        )
    }

    context.logger.info("Failed booking ${domainEvent.bookingId}: ${domainEvent.message}")
}
