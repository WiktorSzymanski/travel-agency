package pl.szymanski.wiktor.ta.infrastructure.scheduler

import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import pl.szymanski.wiktor.ta.command.accommodation.AccommodationCommandHandler
import pl.szymanski.wiktor.ta.command.attraction.AttractionCommandHandler
import pl.szymanski.wiktor.ta.command.commute.CommuteCommandHandler
import pl.szymanski.wiktor.ta.infrastructure.config.SchedulerConfig
import pl.szymanski.wiktor.ta.infrastructure.generator.AccommodationGenerator
import pl.szymanski.wiktor.ta.infrastructure.generator.AttractionGenerator
import pl.szymanski.wiktor.ta.infrastructure.generator.CommuteGenerator
import pl.szymanski.wiktor.ta.infrastructure.generator.GeneratorCHPair

object Scheduler {
    private lateinit var config: SchedulerConfig
    private lateinit var generators: List<GeneratorCHPair<*, *>>

    private var job: Job? = null

    const val MILLIS_IN_SECOND = 1000

    fun init(
        config: SchedulerConfig,
        accommodationCommandHandler: AccommodationCommandHandler,
        attractionCommandHandler: AttractionCommandHandler,
        commuteCommandHandler: CommuteCommandHandler,
    ) {
        this.config = config
        this.generators =
            listOf(
                GeneratorCHPair(
                    CommuteGenerator(
                        config.inAdvanceSeconds,
                        config.creationWindowSeconds,
                        config.commutes,
                    ),
                    commuteCommandHandler,
                ),
                GeneratorCHPair(
                    AccommodationGenerator(
                        config.inAdvanceSeconds,
                        config.creationWindowSeconds,
                        config.accommodations,
                    ),
                    accommodationCommandHandler,
                ),
                GeneratorCHPair(
                    AttractionGenerator(
                        config.inAdvanceSeconds,
                        config.creationWindowSeconds,
                        config.attractions,
                    ),
                    attractionCommandHandler,
                ),
            )
    }

    suspend fun start() =
        coroutineScope {
            if (job != null) {
                return@coroutineScope
            }

            job =
                launch {
                    while (isActive) {
                        generate()
                        delay(config.intervalSeconds * MILLIS_IN_SECOND)
                    }
                }
        }

    fun stop() {
        job?.cancel()
        job = null
    }

    private suspend fun generate() {
        generators.forEach { it.process() }
    }
}
