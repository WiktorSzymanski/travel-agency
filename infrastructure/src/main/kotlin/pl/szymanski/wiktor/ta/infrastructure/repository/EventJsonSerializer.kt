package pl.szymanski.wiktor.ta.infrastructure.repository

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.databind.node.ObjectNode
import io.kurrent.dbclient.EventData
import io.kurrent.dbclient.ResolvedEvent
import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.domain.event.PublishableEvent
import java.time.LocalDateTime
import java.util.UUID

object EventJsonSerializer {
    private val objectMapper = jacksonObjectMapper()

    fun toEventData(event: EventEnvelope<out PublishableEvent>, processAfter: LocalDateTime? = null): EventData {
        val eventJson = objectMapper.writeValueAsBytes(event.event)
        val metadataMap = mutableMapOf<String, Any>(
            "correlationId" to event.metadata.correlationId.toString(),
            "revision" to event.metadata.revision,
            "eventType" to event.eventType
        )
        if (processAfter != null) {
            metadataMap["processAfter"] = processAfter.toString()
        }
        val metadataJson = objectMapper.writeValueAsBytes(metadataMap)
        val builder = EventData.builderAsJson(event.eventType, eventJson)
        builder.eventId(event.event.eventId)
        builder.metadataAsBytes(metadataJson)
        return builder.build()
    }

    fun <T : PublishableEvent> fromResolvedEvent(resolvedEvent: ResolvedEvent, eventType: Class<T>): EventEnvelope<T> {
        val eventBytes = resolvedEvent.event.eventData as ByteArray
        val metadataBytes = resolvedEvent.event.userMetadata as? ByteArray
        val event = objectMapper.readValue(eventBytes, eventType)
        val metadata = if (metadataBytes != null && metadataBytes.isNotEmpty()) {
            val node = objectMapper.readTree(metadataBytes) as ObjectNode
            val correlationId = node["correlationId"]?.asText()?.let { UUID.fromString(it) } ?: UUID.randomUUID()
            val revision = node["revision"]?.asLong() ?: 0L
            pl.szymanski.wiktor.ta.Metadata(correlationId, revision)
        } else {
            pl.szymanski.wiktor.ta.Metadata(UUID.randomUUID(), 0)
        }
        return EventEnvelope(eventType.simpleName ?: "UnknownEvent", event, metadata)
    }
}
