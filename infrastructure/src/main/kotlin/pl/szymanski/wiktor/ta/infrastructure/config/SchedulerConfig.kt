package pl.szymanski.wiktor.ta.infrastructure.config

import kotlinx.serialization.Serializable
import pl.szymanski.wiktor.ta.infrastructure.generator.AccommodationTemplate
import pl.szymanski.wiktor.ta.infrastructure.generator.AttractionTemplate
import pl.szymanski.wiktor.ta.infrastructure.generator.CommuteTemplate

@Serializable
data class SchedulerConfig(
    val intervalSeconds: Long,
    val inAdvanceSeconds: Long,
    val creationWindowSeconds: Long,
    val accommodations: List<AccommodationTemplate>,
    val attractions: List<AttractionTemplate>,
    val commutes: List<CommuteTemplate>,
)
