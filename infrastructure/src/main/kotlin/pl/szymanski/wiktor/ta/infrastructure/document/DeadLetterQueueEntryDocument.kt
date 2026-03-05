package pl.szymanski.wiktor.ta.infrastructure.document

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import pl.szymanski.wiktor.ta.dlq.DeadLetterQueueEntry
import java.time.LocalDateTime
import java.util.UUID

@Document(collection = "dead_letter_queue")
data class DeadLetterQueueEntryDocument(
    @Id
    val id: UUID,
    val message: String,
    val createdAt: String,
) {
    companion object {
        fun fromDomain(entry: DeadLetterQueueEntry) = DeadLetterQueueEntryDocument(
            id = entry.id,
            message = entry.message,
            createdAt = entry.createdAt.toString(),
        )
    }

    fun toDomain() = DeadLetterQueueEntry(
        id = id,
        message = message,
        createdAt = LocalDateTime.parse(createdAt),
    )
}

