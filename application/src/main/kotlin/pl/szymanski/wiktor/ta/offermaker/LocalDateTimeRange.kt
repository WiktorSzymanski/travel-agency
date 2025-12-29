package pl.szymanski.wiktor.ta.offermaker

import java.time.LocalDateTime

data class LocalDateTimeRange(
    val from: LocalDateTime? = null,
    val till: LocalDateTime? = null
)