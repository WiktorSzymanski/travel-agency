package pl.szymanski.wiktor.ta.infrastructure.config

import kotlinx.serialization.Serializable
import pl.szymanski.wiktor.ta.generator.AccommodationTemplate
import pl.szymanski.wiktor.ta.generator.AttractionTemplate
import pl.szymanski.wiktor.ta.generator.CommuteTemplate

@Serializable
data class GeneratorConfig (
    val intervalSeconds: Int,
    val inAdvanceSeconds: Long,
    val creationWindowSeconds: Long,
    val accommodations: List<AccommodationTemplate>,
    val attractions: List<AttractionTemplate>,
    val commutes: List<CommuteTemplate>,
)