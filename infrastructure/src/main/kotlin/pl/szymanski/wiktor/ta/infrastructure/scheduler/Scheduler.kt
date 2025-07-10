package pl.szymanski.wiktor.ta.infrastructure.scheduler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
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
    private lateinit var config: SchedulerConfig
    private lateinit var generators: List<GeneratorRepoPair<*, *>>

    private var job: Job? = null

    const val MILLIS_IN_SECOND = 1000

    fun init(
        config: SchedulerConfig,
        accommodationRepository: AccommodationRepository,
        attractionRepository: AttractionRepository,
        commuteRepository: CommuteRepository,
    ) {
        this.config = config
        this.generators =
            listOf(
                GeneratorRepoPair(
                    CommuteGenerator(
                        config.inAdvanceSeconds,
                        config.creationWindowSeconds,
                        config.commutes,
                    ),
                    commuteRepository,
                ),
                GeneratorRepoPair(
                    AccommodationGenerator(
                        config.inAdvanceSeconds,
                        config.creationWindowSeconds,
                        config.accommodations,
                    ),
                    accommodationRepository,
                ),
                GeneratorRepoPair(
                    AttractionGenerator(
                        config.inAdvanceSeconds,
                        config.creationWindowSeconds,
                        config.attractions,
                    ),
                    attractionRepository,
                ),
            )
    }

    suspend fun start() = coroutineScope {
        if (job != null) {
            return@coroutineScope
        }

        job = launch {
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
