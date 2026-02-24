package pl.szymanski.wiktor.ta.dlq

import java.time.LocalDateTime
import java.util.UUID

data class DeadLetterQueueEntry (
    val id: UUID = UUID.randomUUID(),
    val message: String,
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
