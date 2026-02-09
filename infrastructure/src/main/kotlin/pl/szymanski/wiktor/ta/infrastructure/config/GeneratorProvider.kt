package pl.szymanski.wiktor.ta.infrastructure.config

import io.ktor.server.plugins.di.annotations.Property
import pl.szymanski.wiktor.ta.generator.AccommodationGenerator
import pl.szymanski.wiktor.ta.generator.AttractionGenerator
import pl.szymanski.wiktor.ta.generator.CommuteGenerator

fun provideAccommodationGenerator(
    @Property("generator") generatorConfig: GeneratorConfig
): AccommodationGenerator =
    AccommodationGenerator(
        generatorConfig.inAdvanceSeconds,
        generatorConfig.creationWindowSeconds,
        generatorConfig.accommodations
    )

fun provideAttractionGenerator(
    @Property("generator") generatorConfig: GeneratorConfig
): AttractionGenerator =
    AttractionGenerator(
        generatorConfig.inAdvanceSeconds,
        generatorConfig.creationWindowSeconds,
        generatorConfig.attractions
    )

fun provideCommuteGenerator(
    @Property("generator") generatorConfig: GeneratorConfig
): CommuteGenerator =
    CommuteGenerator(
        generatorConfig.inAdvanceSeconds,
        generatorConfig.creationWindowSeconds,
        generatorConfig.commutes
    )

