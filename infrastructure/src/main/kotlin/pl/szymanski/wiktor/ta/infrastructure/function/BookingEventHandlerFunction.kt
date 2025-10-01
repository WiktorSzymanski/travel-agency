package pl.szymanski.wiktor.ta.infrastructure.function

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.EventGridTrigger
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.QueueTrigger
import kotlinx.coroutines.runBlocking
import pl.szymanski.wiktor.ta.command.FailBookingCommand
import pl.szymanski.wiktor.ta.command.FailCancelBookingCommand
import pl.szymanski.wiktor.ta.command.ReserveTravelOfferCommand
import pl.szymanski.wiktor.ta.command.TravelOfferCommand
import pl.szymanski.wiktor.ta.domain.event.BookingCreatedEvent
import pl.szymanski.wiktor.ta.infrastructure.repository.EventJsonSerializer
import pl.szymanski.wiktor.ta.infrastructure.scheduler.PersistedEvent
import pl.szymanski.wiktor.ta.command.ReleaseTravelOfferCommand
import pl.szymanski.wiktor.ta.domain.event.BookingCancelRequestedEvent
import pl.szymanski.wiktor.ta.infrastructure.scheduler.QueueMessage
import pl.szymanski.wiktor.ta.infrastructure.scheduler.getBookingCommand

@FunctionName("BookingQueueTrigger")
fun bookingQueueTrigger(
    @QueueTrigger(name = "message", queueName = "booking-queue", connection = "AzureWebJobsStorage")
    message: QueueMessage,
    context: ExecutionContext
) = when (val command = message.getBookingCommand()) {
    is FailCancelBookingCommand -> { runBlocking { bookingCommandHandler.handle(command) } }
    is FailBookingCommand -> { runBlocking { bookingCommandHandler.handle(command) }  }
    else -> context.logger.info("INVALID message for queue booking-queue: $message")
}

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

    context.logger.info("User: ${domainEvent.userId} is booking: ${domainEvent}")
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

@FunctionName("BookingCancelRequestedEventHandler")
fun bookingCancelRequestedEventHandler(
    @EventGridTrigger(name = "eventgrid") event: PersistedEvent,
    context: ExecutionContext
) {
    require(event.type == BookingCancelRequestedEvent::class.java.name)
    val domainEvent = EventJsonSerializer.fromJSON(event.domainevent, Class.forName(event.type)) as BookingCancelRequestedEvent

    runBlocking {
        runCatching {
            travelOfferCommandHandler.handle(
                ReleaseTravelOfferCommand(
                    domainEvent.travelOfferId,
                    domainEvent.correlationId!!,
                    domainEvent.bookingId,
                    domainEvent.seat
                ) as TravelOfferCommand,
            )
        }.onFailure { e ->
            context.logger.severe("Failed to release travel offer event: $domainEvent, error: ${e.message}")
        }
    }

    context.logger.info("Requested cancel booking ${domainEvent.bookingId} for travel offer ${domainEvent.travelOfferId}")
}
