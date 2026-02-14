package pl.szymanski.wiktor.ta.infrastructure.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import pl.szymanski.wiktor.ta.generator.AccommodationGenerator
import pl.szymanski.wiktor.ta.generator.AttractionGenerator
import pl.szymanski.wiktor.ta.generator.CommuteGenerator

@Configuration
class GeneratorConfiguration(
    private val generatorConfig: GeneratorConfig
) {
    @Bean
    fun accommodationGenerator(): AccommodationGenerator =
        AccommodationGenerator(
            generatorConfig.inAdvanceSeconds,
            generatorConfig.creationWindowSeconds,
            generatorConfig.accommodations
        )

    @Bean
    fun attractionGenerator(): AttractionGenerator =
        AttractionGenerator(
            generatorConfig.inAdvanceSeconds,
            generatorConfig.creationWindowSeconds,
            generatorConfig.attractions
        )

    @Bean
    fun commuteGenerator(): CommuteGenerator =
        CommuteGenerator(
            generatorConfig.inAdvanceSeconds,
            generatorConfig.creationWindowSeconds,
            generatorConfig.commutes
        )
}
