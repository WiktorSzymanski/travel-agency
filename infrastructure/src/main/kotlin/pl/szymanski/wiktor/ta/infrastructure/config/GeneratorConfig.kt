package pl.szymanski.wiktor.ta.infrastructure.config

import kotlinx.serialization.Serializable
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import pl.szymanski.wiktor.ta.generator.AccommodationTemplate
import pl.szymanski.wiktor.ta.generator.AttractionTemplate
import pl.szymanski.wiktor.ta.generator.CommuteTemplate

@Component
@ConfigurationProperties(prefix = "generator")
@Serializable
data class GeneratorConfig(
    var intervalSeconds: Int = 10,
    var inAdvanceSeconds: Long = 30,
    var creationWindowSeconds: Long = 10,
    var accommodations: List<AccommodationTemplate> = emptyList(),
    var attractions: List<AttractionTemplate> = emptyList(),
    var commutes: List<CommuteTemplate> = emptyList(),
)
