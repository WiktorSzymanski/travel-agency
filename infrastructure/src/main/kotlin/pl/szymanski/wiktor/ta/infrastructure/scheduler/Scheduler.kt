package pl.szymanski.wiktor.ta.infrastructure.scheduler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import pl.szymanski.wiktor.ta.domain.repository.AccommodationRepository
import pl.szymanski.wiktor.ta.domain.repository.AttractionRepository
import pl.szymanski.wiktor.ta.domain.repository.CommuteRepository
import pl.szymanski.wiktor.ta.infrastructure.config.SchedulerConfig
import pl.szymanski.wiktor.ta.infrastructure.generator.AccommodationGenerator
import pl.szymanski.wiktor.ta.infrastructure.generator.AttractionGenerator
import pl.szymanski.wiktor.ta.infrastructure.generator.CommuteGenerator
import pl.szymanski.wiktor.ta.infrastructure.generator.GeneratorRepoPair
import kotlin.coroutines.CoroutineContext


object Scheduler {
    private lateinit var coroutineContext: CoroutineContext
    private lateinit var config: SchedulerConfig
    private lateinit var generators: List<GeneratorRepoPair<*, *>>

    private var job: Job? = null

    const val TO_SECONDS = 1000

    fun init(
        config: SchedulerConfig,
        accommodationRepository: AccommodationRepository,
        attractionRepository: AttractionRepository,
        commuteRepository: CommuteRepository,
        coroutineContext: CoroutineContext = Dispatchers.Default,
    ) {
        this.coroutineContext = coroutineContext
        this.config = config
        this.generators =
            listOf(
                GeneratorRepoPair(
                    CommuteGenerator(
                        config.futureDurationSeconds,
                        config.commutes,
                    ),
                    commuteRepository,
                ),
                GeneratorRepoPair(
                    AccommodationGenerator(
                        config.futureDurationSeconds,
                        config.accommodations,
                    ),
                    accommodationRepository,
                ),
                GeneratorRepoPair(
                    AttractionGenerator(
                        config.futureDurationSeconds,
                        config.attractions,
                    ),
                    attractionRepository,
                ),
            )
    }

    fun start() {
        if (job != null) {
            return
        }

        job =
            CoroutineScope(coroutineContext).launch {
                while (isActive) {
                    generate()
                    delay(config.intervalSeconds * TO_SECONDS)
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
