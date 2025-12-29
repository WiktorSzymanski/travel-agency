package pl.szymanski.wiktor.ta.offerMaker

import java.time.LocalDateTime

data class LocalDateTimeRange(
    val from: LocalDateTime? = null,
    val till: LocalDateTime? = null
)