package pl.szymanski.wiktor.ta.infrastructure.document

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import pl.szymanski.wiktor.ta.outbox.OutboxEntry
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

@Document(collection = "outbox")
data class OutboxEntryDocument(
    @Id
    val eventId: UUID,
    val eventType: String,
    val payload: String,
    val published: Boolean,
    val publishedAt: Instant?,
    val createdAt: Instant,
    val processAfter: LocalDateTime?,
)

