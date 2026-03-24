package pl.szymanski.wiktor.ta.infrastructure.outbox

import io.kurrent.dbclient.AppendToStreamOptions
import io.kurrent.dbclient.EventData
import io.kurrent.dbclient.KurrentDBClient
import io.kurrent.dbclient.StreamState
import tools.jackson.databind.ObjectMapper
import kotlinx.coroutines.future.await
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.domain.event.PublishableEvent
import pl.szymanski.wiktor.ta.outbox.OutboxEntry
import pl.szymanski.wiktor.ta.outbox.OutboxPort
import pl.szymanski.wiktor.ta.repository.CommandRepository
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

@Service
@Primary
class OutboxPortImpl(
    private val kurrentDBClient: KurrentDBClient,
    private val objectMapper: ObjectMapper,
) : OutboxPort {

    override suspend fun <T, R> create(
        entity: T,
        event: PublishableEvent,
        metadata: Metadata,
        repository: CommandRepository<T, R>
    ) {
        save(entity, listOf(event), metadata, repository)
    }

    override suspend fun <T, R> save(
        entity: T,
        events: List<PublishableEvent>,
        metadata: Metadata,
        repository: CommandRepository<T, R>
    ) {
        if (events.isEmpty()) return

        val aggregateId = events.first().entityId.toString()
        val aggregateType = entity!!::class.simpleName!!
        val streamName = "$aggregateType-$aggregateId"

        val eventDataList: List<EventData> = events.map { event: PublishableEvent ->
            val eventId = event.eventId
            val eventType = event::class.simpleName!!
            val dataBytes = objectMapper.writeValueAsBytes(event)
            val metaBytes = objectMapper.writeValueAsBytes(metadata)
            val builder = EventData.builderAsJson(eventType, dataBytes)
            builder.eventId(eventId)
            builder.metadataAsBytes(metaBytes)
            builder.build()
        }

        val streamState = if (metadata.revision <= 0L) {
            StreamState.noStream()
        } else {
            StreamState.streamRevision(metadata.revision - 1)
        }

        kurrentDBClient.appendToStream(
            streamName,
            AppendToStreamOptions.get().streamState(streamState),
            eventDataList.iterator()
        ).await()
    }
// TODO: make it work after delay
    override suspend fun delayEvent(event: PublishableEvent, processAfter: LocalDateTime) {
        val streamName = "delayed-events"
        val dataBytes = objectMapper.writeValueAsBytes(event)
        val deliverAtInstant = processAfter.atZone(ZoneOffset.UTC).toInstant()
        val nowInstant = LocalDateTime.now().atZone(ZoneOffset.UTC).toInstant()
        val delayMillis = Duration.between(nowInstant, deliverAtInstant).toMillis().coerceAtLeast(0L)

        val metaBytes = objectMapper.writeValueAsBytes(
            mapOf(
                "processAfter" to processAfter.toString(),
                "deliveryTime" to processAfter.toString(),
                "deliverAt" to deliverAtInstant.toString(),
                "delayMillis" to delayMillis
            )
        )
        val builder = EventData.builderAsJson(event::class.simpleName!!, dataBytes)
        builder.eventId(event.eventId)
        builder.metadataAsBytes(metaBytes)
        val eventData = builder.build()

        kurrentDBClient.appendToStream(
            streamName,
            AppendToStreamOptions.get(),
            listOf(eventData).iterator()
        ).await()
    }

    override suspend fun getPendingEntries(limit: Int): List<OutboxEntry> {
        return emptyList()
    }

    override suspend fun markAsPublished(eventId: UUID) {
    }
}
