package pl.szymanski.wiktor.ta

import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.UUID


@Serializable
data class Metadata (
    val correlationId: @Serializable(with = pl.szymanski.wiktor.ta.domain.UUIDSerializer::class) UUID,
    val revision: Long,
)

data class LocalDateTimeRange(
    val from: LocalDateTime? = null,
    val till: LocalDateTime? = null
)