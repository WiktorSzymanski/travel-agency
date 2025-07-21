package pl.szymanski.wiktor.ta.infrastructure.scheduler

import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import pl.szymanski.wiktor.ta.command.travelOffer.TravelOfferCommandHandler
import pl.szymanski.wiktor.ta.domain.repository.AccommodationRepository
import pl.szymanski.wiktor.ta.domain.repository.AttractionRepository
import pl.szymanski.wiktor.ta.domain.repository.CommuteRepository
import pl.szymanski.wiktor.ta.domain.repository.TravelOfferRepository
import pl.szymanski.wiktor.ta.infrastructure.config.OfferSchedulerConfig
import pl.szymanski.wiktor.ta.offerMaker.offerMaker

object OfferScheduler {
    private lateinit var config: OfferSchedulerConfig

    private lateinit var accommodationRepository: AccommodationRepository
    private lateinit var attractionRepository: AttractionRepository
    private lateinit var commuteRepository: CommuteRepository
    private lateinit var travelOfferCommandHandler: TravelOfferCommandHandler

    private var job: Job? = null

    const val MILLIS_IN_SECOND = 1000

    fun init(
        config: OfferSchedulerConfig,
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
