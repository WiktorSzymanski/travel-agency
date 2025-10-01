package pl.szymanski.wiktor.ta.infrastructure.function

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.EventGridTrigger
import com.microsoft.azure.functions.annotation.FunctionName
import kotlinx.coroutines.runBlocking
import pl.szymanski.wiktor.ta.command.BookingCommand
import pl.szymanski.wiktor.ta.command.CancelBookingCommand
import pl.szymanski.wiktor.ta.command.ProcessCancelBookingCommand
import pl.szymanski.wiktor.ta.command.FailCancelBookingCommand
import pl.szymanski.wiktor.ta.command.CancelBookTravelOfferCommand
import pl.szymanski.wiktor.ta.command.TravelOfferCommand
import pl.szymanski.wiktor.ta.event.BookingCancelSagaStartedEvent
import pl.szymanski.wiktor.ta.event.BookingCancelSagaCompletedEvent
import pl.szymanski.wiktor.ta.event.BookingCancelSagaFailedEvent
import pl.szymanski.wiktor.ta.infrastructure.repository.EventJsonSerializer
import pl.szymanski.wiktor.ta.infrastructure.scheduler.PersistedEvent

@FunctionName("CancelBookingSagaStartedEventHandler")
fun cancelBookingSagaStartedEventHandler(
    @EventGridTrigger(name = "eventgrid") event: PersistedEvent,
    context: ExecutionContext
) {
    require(event.type == BookingCancelSagaStartedEvent::class.java.name)
    val domainEvent = EventJsonSerializer.fromJSON(event.domainevent, Class.forName(event.type)) as BookingCancelSagaStartedEvent

    runBlocking {
        bookingCommandHandler.handle(
            ProcessCancelBookingCommand(
                bookingId = domainEvent.bookingId,
                correlationId = domainEvent.correlationId!!,
            ) as BookingCommand
        )
    }

    context.logger.info("Processed CancelBookingSagaStarted for booking ${domainEvent.bookingId}")
}

@FunctionName("CancelBookingSagaCompletedEventHandler")
fun cancelBookingSagaCompletedEventHandler(
    @EventGridTrigger(name = "eventgrid") event: PersistedEvent,
    context: ExecutionContext
) {
    require(event.type == BookingCancelSagaCompletedEvent::class.java.name)
    val domainEvent = EventJsonSerializer.fromJSON(event.domainevent, Class.forName(event.type)) as BookingCancelSagaCompletedEvent

    runBlocking {
        bookingCommandHandler.handle(
            CancelBookingCommand(
                bookingId = domainEvent.bookingId,
                correlationId = domainEvent.correlationId!!,
            ) as BookingCommand
        )
    }

    context.logger.info("Canceled booking ${domainEvent.bookingId}")
}

@FunctionName("CancelBookingSagaCompletedEventHandler2")
fun cancelBookingSagaCompletedEventHandler2(
    @EventGridTrigger(name = "eventgrid") event: PersistedEvent,
    context: ExecutionContext
) {
    require(event.type == BookingCancelSagaCompletedEvent::class.java.name)
    val domainEvent = EventJsonSerializer.fromJSON(event.domainevent, Class.forName(event.type)) as BookingCancelSagaCompletedEvent

    runBlocking {
        travelOfferCommandHandler.handle(
            CancelBookTravelOfferCommand(
                travelOfferId = domainEvent.travelOfferId,
                correlationId = domainEvent.correlationId!!,
                bookingId = domainEvent.bookingId,
                seat = domainEvent.seat
            ) as TravelOfferCommand
        )
    }

    context.logger.info("Canceled booking on travel offer ${domainEvent.travelOfferId}")
}

@FunctionName("CancelBookingSagaFailedEventHandler")
fun cancelBookingSagaFailedEventHandler(
    @EventGridTrigger(name = "eventgrid") event: PersistedEvent,
    context: ExecutionContext
) {
    require(event.type == BookingCancelSagaFailedEvent::class.java.name)
    val domainEvent = EventJsonSerializer.fromJSON(event.domainevent, Class.forName(event.type)) as BookingCancelSagaFailedEvent

    runBlocking {
        bookingCommandHandler.handle(
            FailCancelBookingCommand(
                bookingId = domainEvent.bookingId,
                correlationId = domainEvent.correlationId!!,
                message = domainEvent.message
            ) as BookingCommand
        )
    }

    context.logger.info("Fail cancel booking ${domainEvent.bookingId}: ${domainEvent.message}")
}
