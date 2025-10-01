package pl.szymanski.wiktor.ta.infrastructure.scheduler

import com.azure.storage.queue.QueueClient
import com.azure.storage.queue.QueueClientBuilder
import pl.szymanski.wiktor.ta.command.BookingCommand
import pl.szymanski.wiktor.ta.domain.event.Event
import pl.szymanski.wiktor.ta.infrastructure.repository.EventJsonSerializer
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

data class QueueMessage(
    val type: String,
    val message: String
)

fun QueueMessage.getEvent(): Event {
    return EventJsonSerializer.fromJSON(this.message, Class.forName(this.type)) as Event
}

fun QueueMessage.getBookingCommand(): BookingCommand {
    return EventJsonSerializer.fromJSON(this.message, Class.forName(this.type)) as BookingCommand
}

object QueueClientProvider {
    private val connectionString = System.getenv("AzureWebJobsStorage") ?: throw kotlin.IllegalStateException("Missing AzureWebJobsStorage env var")

    private val dateMetClient: QueueClient by lazy {
        QueueClientBuilder()
            .connectionString(connectionString)
            .queueName("date-met-queue")
            .buildClient()
    }

    private val bookingClient: QueueClient by lazy {
        QueueClientBuilder()
            .connectionString(connectionString)
            .queueName("booking-queue")
            .buildClient()
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun sendBookingCommand(command: BookingCommand) {
        val queueMessage = QueueMessage(command::class.java.name, EventJsonSerializer.toJSON(command))

        bookingClient.createIfNotExists()

        bookingClient.sendMessage(
            Base64.encode(EventJsonSerializer.toBytes(queueMessage)),
        )
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun sendDelayedEventMessage(event: Event, targetDateTime: LocalDateTime) {
        val now = LocalDateTime.now()
        val zone = ZoneId.systemDefault()
        val nowInstant = now.atZone(zone).toInstant()
        val targetInstant = targetDateTime.plusSeconds(2).atZone(zone).toInstant()
        val duration = Duration.between(nowInstant, targetInstant)

        val messageContent = EventJsonSerializer.toJSON(event)
        val queueMessage = QueueMessage(event::class.java.name, messageContent)

        dateMetClient.createIfNotExists()

        dateMetClient.sendMessageWithResponse(
            Base64.encode(EventJsonSerializer.toBytes(queueMessage)),
            if (duration.isNegative) Duration.ZERO else duration,
            null,
            null,
            null
        )
    }
}