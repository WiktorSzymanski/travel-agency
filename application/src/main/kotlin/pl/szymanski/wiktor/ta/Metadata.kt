package pl.szymanski.wiktor.ta

import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.UUID



data class Metadata (
    val correlationId: UUID,
    val revision: Long,
)

data class LocalDateTimeRange(
    val from: LocalDateTime? = null,
    val till: LocalDateTime? = null
)