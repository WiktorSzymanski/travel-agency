package pl.szymanski.wiktor.ta.infrastructure.outbox

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tools.jackson.databind.json.JsonMapper
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository
import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.domain.event.PublishableEvent
import pl.szymanski.wiktor.ta.infrastructure.document.OutboxEntryDocument
import pl.szymanski.wiktor.ta.infrastructure.repository.interfaces.OutboxDocumentMongoRepository
import pl.szymanski.wiktor.ta.outbox.OutboxEntry
import java.time.LocalDateTime
import java.util.UUID

@Repository
class OutboxRepositoryMongo(
    private val outboxDocumentMongoRepository: OutboxDocumentMongoRepository,
    private val objectMapper: JsonMapper,
) {

    suspend fun save(entry: OutboxEntry) {
        val document = toDocument(entry)
        withContext(Dispatchers.IO) {
            outboxDocumentMongoRepository.save(document)
        }
    }

    fun saveBlocking(entry: OutboxEntry) {
        val document = toDocument(entry)
        outboxDocumentMongoRepository.save(document)
    }

    private fun toDocument(entry: OutboxEntry): OutboxEntryDocument {
        val payload = objectMapper.writeValueAsString(entry.eventEnvelope)
        return OutboxEntryDocument(
            eventId = entry.eventId,
            eventType = entry.eventEnvelope.eventType,
            payload = payload,
            published = entry.published,
            publishedAt = entry.publishedAt,
            createdAt = entry.createdAt,
            processAfter = entry.processAfter,
        )
    }

    suspend fun getPendingEntries(limit: Int): List<OutboxEntry> {
        val now = LocalDateTime.now()
        return withContext(Dispatchers.IO) {
            outboxDocumentMongoRepository.findByPublishedFalseAndProcessAfterLessThanEqualOrderByCreatedAtAsc(
                now,
                PageRequest.of(0, limit)
            )
        }.map { doc ->
            @Suppress("UNCHECKED_CAST")
            val envelope = objectMapper.readValue(doc.payload, EventEnvelope::class.java) as EventEnvelope<PublishableEvent>
            OutboxEntry(
                eventId = doc.eventId,
                eventEnvelope = envelope,
                published = doc.published,
                publishedAt = doc.publishedAt,
                createdAt = doc.createdAt,
                processAfter = doc.processAfter,
            )
        }
    }

    suspend fun markAsPublished(eventId: UUID) {
        withContext(Dispatchers.IO) {
            outboxDocumentMongoRepository.findById(eventId).ifPresent { doc ->
                outboxDocumentMongoRepository.save(doc.copy(published = true))
            }
        }
    }
}