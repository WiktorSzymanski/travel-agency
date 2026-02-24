package pl.szymanski.wiktor.ta.infrastructure.outbox

import org.bson.codecs.pojo.annotations.BsonId
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

data class OutboxEntryDto(
    @BsonId val eventId: UUID,
    val eventType: String,
    val payload: String,
    val published: Boolean,
    val publishedAt: Instant?,
    val createdAt: Instant,
    val processAfter: LocalDateTime?,
)

