package pl.szymanski.wiktor.ta.infrastructure.function

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.QueueTrigger
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
import pl.szymanski.wiktor.ta.infrastructure.scheduler.QueueMessage
import pl.szymanski.wiktor.ta.infrastructure.scheduler.getEvent

@FunctionName("DateMetQueueTrigger")
fun dateMetQueueTrigger(
    @QueueTrigger(name = "message", queueName = "date-met-queue", connection = "AzureWebJobsStorage")
    message: QueueMessage,
    context: ExecutionContext
) = when (val event = message.getEvent()) {
        is AccommodationDateMetEvent -> runBlocking {
                accommodationCommandHandler.handle(
                    ExpireAccommodationCommand(
                        accommodationId = event.accommodationId,
                        correlationId = event.correlationId!!,
                    ) as AccommodationCommand,
                )
            context.logger.info("Accommodation date met: ${event.accommodationId}")
        }
        is AttractionDateMetEvent -> runBlocking {
            attractionCommandHandler.handle(
                ExpireAttractionCommand(
                    attractionId = event.attractionId,
                    correlationId = event.correlationId!!,
                ) as AttractionCommand,
            )
            context.logger.info("Attraction date met: ${event.attractionId}")
        }
        is CommuteDateMetEvent -> runBlocking {
            commuteCommandHandler.handle(
                ExpireCommuteCommand(
                    commuteId = event.commuteId,
                    correlationId = event.correlationId!!,
                ) as CommuteCommand,
            )
            context.logger.info("Commute date met: ${event.commuteId}")
        }
        else -> context.logger.info("INVALID event for date-met-queue: $event")
    }