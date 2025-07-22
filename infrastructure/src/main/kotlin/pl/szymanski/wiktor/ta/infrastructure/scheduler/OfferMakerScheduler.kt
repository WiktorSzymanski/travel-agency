package pl.szymanski.wiktor.ta.infrastructure.scheduler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import pl.szymanski.wiktor.ta.commandHandler.TravelOfferCommandHandler
import pl.szymanski.wiktor.ta.domain.repository.AccommodationRepository
import pl.szymanski.wiktor.ta.domain.repository.AttractionRepository
import pl.szymanski.wiktor.ta.domain.repository.CommuteRepository
import pl.szymanski.wiktor.ta.infrastructure.config.OfferMakerSchedulerConfig
import pl.szymanski.wiktor.ta.offerMaker.OfferMaker

object OfferMakerScheduler {
    private lateinit var config: OfferMakerSchedulerConfig
    private lateinit var offerMaker: OfferMaker

    private var job: Job? = null

    const val MILLIS_IN_SECOND = 1000

    fun init(
        config: OfferMakerSchedulerConfig,
        accommodationRepository: AccommodationRepository,
        attractionRepository: AttractionRepository,
        commuteRepository: CommuteRepository,
        travelOfferCommandHandler: TravelOfferCommandHandler,
    ) {
        this.config = config

        this.offerMaker = OfferMaker(
            accommodationRepository,
            attractionRepository,
            commuteRepository,
            travelOfferCommandHandler
        )
    }

    fun start(
        scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
    ) {
        if (job != null) return
        job = scope.launch {
            while (isActive) {
                offerMaker.makeOffers()
                delay(config.intervalSeconds * MILLIS_IN_SECOND)
            }
        }
    }

    fun stop() = job?.cancel().also { job = null }
}
