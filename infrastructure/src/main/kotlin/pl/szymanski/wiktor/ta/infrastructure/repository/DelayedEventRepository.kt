package pl.szymanski.wiktor.ta.infrastructure.repository

import kotlinx.serialization.Serializable
import pl.szymanski.wiktor.ta.domain.LocalDateTimeSerializer
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class DelayedEvent(
    val id: String = UUID.randomUUID().toString(),
    val payload: String,
    val topic: String,
    val key: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val scheduledAt: LocalDateTime,
    val processed: Boolean = false
)

interface DelayedEventRepository {
    suspend fun save(event: DelayedEvent)
    suspend fun findReadyToPublish(now: LocalDateTime): List<DelayedEvent>
    suspend fun markAsProcessed(id: String)
}
