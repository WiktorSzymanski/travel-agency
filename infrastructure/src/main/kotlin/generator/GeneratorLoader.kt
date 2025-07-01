package generator

import com.fasterxml.jackson.module.kotlin.readValue
import loader.YamlConfigLoader
import pl.szymanski.wiktor.ta.domain.repository.AccommodationRepository
import pl.szymanski.wiktor.ta.domain.repository.AttractionRepository
import pl.szymanski.wiktor.ta.domain.repository.CommuteRepository
import scheduler.DataSchedulerConfig
import scheduler.GeneratorType
import java.io.File

class GeneratorLoader(
    configFilePath: String,
    private val accommodationRepository: AccommodationRepository,
    private val attractionRepository: AttractionRepository,
    private val commuteRepository: CommuteRepository,
    private val yamlConfigLoader: YamlConfigLoader = YamlConfigLoader(),
) {
    private var config: DataSchedulerConfig =
        yamlConfigLoader.mapper.readValue(
            File(configFilePath),
            DataSchedulerConfig::class.java,
        )
    private val generators = mutableListOf<GeneratorRepoPair<*, *>>()

    init {
        config.generators.forEach { generatorConfig ->
            when (generatorConfig.type) {
                GeneratorType.ACCOMMODATION ->
                    generators.add(
                        GeneratorRepoPair(
                            AccommodationGenerator(
                                yamlConfigLoader,
                                generatorConfig.yamlFilePath,
                                generatorConfig.futureDurationSeconds,
                            ),
                            accommodationRepository,
                        ),
                    )
                GeneratorType.ATTRACTION ->
                    generators.add(
                        GeneratorRepoPair(
                            AttractionGenerator(
                                yamlConfigLoader,
                                generatorConfig.yamlFilePath,
                                generatorConfig.futureDurationSeconds,
                            ),
                            attractionRepository,
                        ),
                    )
                GeneratorType.COMMUTE ->
                    generators.add(
                        GeneratorRepoPair(
                            CommuteGenerator(
                                yamlConfigLoader,
                                generatorConfig.yamlFilePath,
                                generatorConfig.futureDurationSeconds,
                            ),
                            commuteRepository,
                        ),
                    )
            }
        }
    }

    fun getGenerators(): List<GeneratorRepoPair<*, *>> = generators.toList()

    fun getInterval(): Long = config.intervalSeconds
}
