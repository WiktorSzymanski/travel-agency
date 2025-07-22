package pl.szymanski.wiktor.ta.infrastructure.scheduler

import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import pl.szymanski.wiktor.ta.commandHandler.TravelOfferCommandHandler
import pl.szymanski.wiktor.ta.domain.repository.AccommodationRepository
import pl.szymanski.wiktor.ta.domain.repository.AttractionRepository
import pl.szymanski.wiktor.ta.domain.repository.CommuteRepository
import pl.szymanski.wiktor.ta.infrastructure.config.OfferMakerSchedulerConfig
import pl.szymanski.wiktor.ta.offerMaker.offerMaker

object OfferMakerScheduler {
    private lateinit var config: OfferMakerSchedulerConfig

    private lateinit var accommodationRepository: AccommodationRepository
    private lateinit var attractionRepository: AttractionRepository
    private lateinit var commuteRepository: CommuteRepository
    private lateinit var travelOfferCommandHandler: TravelOfferCommandHandler

    private var job: Job? = null

    const val MILLIS_IN_SECOND = 1000

    fun init(
        config: OfferMakerSchedulerConfig,
        accommodationRepository: AccommodationRepository,
        attractionRepository: AttractionRepository,
        commuteRepository: CommuteRepository,
        travelOfferCommandHandler: TravelOfferCommandHandler,
    ) {
        this.accommodationRepository = accommodationRepository
        this.attractionRepository = attractionRepository
        this.commuteRepository = commuteRepository
        this.travelOfferCommandHandler = travelOfferCommandHandler

        this.config = config
    }

    suspend fun start() =
        coroutineScope {
            if (job != null) {
                return@coroutineScope
            }

            job =
                launch {
                    while (isActive) {
                        offerMaker(
                            accommodationRepository,
                            attractionRepository,
                            commuteRepository,
                            travelOfferCommandHandler,
                        )
                        delay(config.intervalSeconds * MILLIS_IN_SECOND)
                    }
                }
        }

    fun stop() {
        job?.cancel()
        job = null
    }
}
