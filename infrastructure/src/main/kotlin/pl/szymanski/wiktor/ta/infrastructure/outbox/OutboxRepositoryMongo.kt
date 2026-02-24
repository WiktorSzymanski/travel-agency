package pl.szymanski.wiktor.ta.infrastructure.outbox

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.json.Json
import org.springframework.stereotype.Repository
import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.domain.event.PublishableEvent
import pl.szymanski.wiktor.ta.infrastructure.config.MongoConfiguration
import pl.szymanski.wiktor.ta.outbox.OutboxEntry
import java.time.Instant
import java.util.UUID

// TODO: interface

@Repository
class OutboxRepositoryMongo(
    mongoConfiguration: MongoConfiguration
) {
    private val json = Json {
        ignoreUnknownKeys = true
        serializersModule = publishableEventModule
    }

    private val collection = mongoConfiguration.mongoClient()
        .getDatabase(mongoConfiguration.mongoConfig.dbName)
        .getCollection<OutboxEntryDto>("outbox")

    suspend fun save(entry: OutboxEntry) {
        val payload = json.encodeToString(entry.eventEnvelope)
        val dto = OutboxEntryDto(
            eventId = entry.eventId,
            eventType = entry.eventEnvelope.eventType,
            payload = payload,
            published = entry.published,
            publishedAt = entry.publishedAt,
            createdAt = entry.createdAt,
            processAfter = entry.processAfter,
        )
        collection.insertOne(dto)
    }

    suspend fun getPendingEntries(limit: Int): List<OutboxEntry> {
        val now = Instant.now()
        return collection.find(
            Filters.and(
                Filters.eq("published", false),
                Filters.or(
                    Filters.eq("processAfter", null),
                    Filters.lte("processAfter", now)
                )
            )
        ).limit(limit).map { dto ->
            val envelope = json.decodeFromString<EventEnvelope<PublishableEvent>>(dto.payload)
            OutboxEntry(
                eventId = dto.eventId,
                eventEnvelope = envelope,
                published = dto.published,
                publishedAt = dto.publishedAt,
                createdAt = dto.createdAt,
                processAfter = dto.processAfter,
            )
        }.toList()
    }

    suspend fun markAsPublished(eventId: UUID) {
        collection.updateOne(Filters.eq("_id", eventId), Updates.set("published", true))
    }
}