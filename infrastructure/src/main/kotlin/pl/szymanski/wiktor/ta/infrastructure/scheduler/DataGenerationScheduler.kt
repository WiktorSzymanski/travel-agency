package pl.szymanski.wiktor.ta.infrastructure.scheduler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import pl.szymanski.wiktor.ta.commandHandler.AccommodationCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.AttractionCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.CommuteCommandHandler
import pl.szymanski.wiktor.ta.infrastructure.config.DataGenerationSchedulerConfig
import pl.szymanski.wiktor.ta.infrastructure.generator.AccommodationGenerator
import pl.szymanski.wiktor.ta.infrastructure.generator.AttractionGenerator
import pl.szymanski.wiktor.ta.infrastructure.generator.CommuteGenerator
import pl.szymanski.wiktor.ta.infrastructure.generator.GeneratorCHPair

object DataGenerationScheduler {
    private lateinit var config: DataGenerationSchedulerConfig
    private lateinit var generators: List<GeneratorCHPair<*, *>>

    private var job: Job? = null

    const val MILLIS_IN_SECOND = 1000

    fun init(
        config: DataGenerationSchedulerConfig,
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

    fun start(
        scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
    ) {
        if (job != null) return
        job = scope.launch {
            generate()
            while (isActive) {
                delay(config.intervalSeconds * MILLIS_IN_SECOND)
                generate()
            }
        }
    }

    fun stop() = job?.cancel().also { job = null }

    private suspend fun generate() {
        generators.forEach { it.process() }
    }
}
