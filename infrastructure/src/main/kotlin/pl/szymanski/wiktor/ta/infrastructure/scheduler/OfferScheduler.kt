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
import pl.szymanski.wiktor.ta.domain.repository.TravelOfferRepository
import pl.szymanski.wiktor.ta.infrastructure.config.OfferSchedulerConfig
import pl.szymanski.wiktor.ta.offerMaker.offerMaker
import kotlin.coroutines.CoroutineContext

object OfferScheduler {
    private lateinit var coroutineContext: CoroutineContext
    private lateinit var config: OfferSchedulerConfig

    private lateinit var accommodationRepository: AccommodationRepository
    private lateinit var attractionRepository: AttractionRepository
    private lateinit var commuteRepository: CommuteRepository
    private lateinit var travelOfferRepository: TravelOfferRepository

    private var job: Job? = null

    const val MILLIS_IN_SECOND = 1000

    fun init(
        config: OfferSchedulerConfig,
        accommodationRepository: AccommodationRepository,
        attractionRepository: AttractionRepository,
        commuteRepository: CommuteRepository,
        travelOfferRepository: TravelOfferRepository,
        coroutineContext: CoroutineContext = Dispatchers.Default,
    ) {
        this.accommodationRepository = accommodationRepository
        this.attractionRepository = attractionRepository
        this.commuteRepository = commuteRepository
        this.travelOfferRepository = travelOfferRepository

        this.coroutineContext = coroutineContext
        this.config = config
    }

    fun start() {
        if (job != null) {
            return
        }

        job =
            CoroutineScope(coroutineContext).launch {
                while (isActive) {
                    offerMaker(
                        accommodationRepository,
                        attractionRepository,
                        commuteRepository,
                        travelOfferRepository,
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
