package pl.szymanski.wiktor.ta.offerMaker

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.command.CreateTravelOfferCommand
import pl.szymanski.wiktor.ta.command.TravelOfferCommand
import pl.szymanski.wiktor.ta.commandHandler.TravelOfferCommandHandler
import pl.szymanski.wiktor.ta.domain.AccommodationStatusEnum
import pl.szymanski.wiktor.ta.domain.AttractionStatusEnum
import pl.szymanski.wiktor.ta.domain.CommuteStatusEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.event.TravelOfferExpiredEvent
import pl.szymanski.wiktor.ta.queryRepository.AccommodationQueryRepository
import pl.szymanski.wiktor.ta.queryRepository.AttractionQueryRepository
import pl.szymanski.wiktor.ta.queryRepository.CommuteQueryRepository
import pl.szymanski.wiktor.ta.timeMet
import java.time.Duration
import java.util.UUID

class OfferMaker(
    private val accommodationRepository: AccommodationQueryRepository,
    private val attractionRepository: AttractionQueryRepository,
    private val commuteRepository: CommuteQueryRepository,
    private val travelOfferCommandHandler: TravelOfferCommandHandler,
) {
    private val offerHashes = mutableListOf<Int>()

    companion object {
        private val log = LoggerFactory.getLogger(OfferMaker::class.java)
        private const val EXPIRED_HASH_POP_DELAY_MS: Long = 10_000
    }

    init {
        popExpiredHashes()
    }

    private fun popExpiredHashes(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) {
        scope.launch {
            EventBus.subscribe<TravelOfferExpiredEvent> {
                delay(EXPIRED_HASH_POP_DELAY_MS)
                offerHashes.remove(
                    Triple(
                        it.commuteId,
                        it.accommodationId,
                        it.attractionId,
                    ).hashCode(),
                )
            }
        }
    }

    suspend fun makeOffers() =
        coroutineScope {
            val (commutes, accommodations, attractions) = collectData()
            val offerTriples = createOfferTriples(commutes, accommodations, attractions)

            if (offerTriples.isNotEmpty()) {
                offerTriples.forEach { offerTriple ->
                    val offerMatchHash = offerTriple.toIds().hashCode()
                    if (!offerHashes.contains(offerMatchHash)) {
                        launch {
                            runCatching {
                                travelOfferCommandHandler.handle(offerTriple.toCommand() as TravelOfferCommand)
                            }.exceptionOrNull()?.let {
                                if (it.message?.contains("E11000 duplicate key error collection") ?: false) {
                                    log.info(
                                        "ERROR: E11000 duplicate key error collection while creating offer for $offerTriple triple. Maybe should save offers in bulk?",
                                    )
                                } else {
                                    throw it
                                }
                            }
                        }
                        offerHashes.add(offerMatchHash)
                    }
                }
            }
        }

    private suspend fun collectData() =
        coroutineScope {
            val accommodations = async { accommodationRepository.findAllByStatus(AccommodationStatusEnum.AVAILABLE) }
            val attractions = async { attractionRepository.findAllByStatus(AttractionStatusEnum.SCHEDULED) }
            val commutes = async { commuteRepository.findAllByStatus(CommuteStatusEnum.SCHEDULED) }

            val accommodationPairs = async { accommodations.await().partition { it.timeMet() } }
            val attractionPairs = async { attractions.await().partition { it.timeMet() } }
            val commutePairs = async { commutes.await().partition { it.timeMet() } }

            val (_, validAccommodations) = accommodationPairs.await()
            val (_, validAttractions) = attractionPairs.await()
            val (_, validCommutes) = commutePairs.await()

            return@coroutineScope Triple(validCommutes, validAccommodations, validAttractions)
        }

    private fun createOfferTriples(
        commutes: List<Commute>,
        accommodations: List<Accommodation>,
        attractions: List<Attraction>,
        creationWindowSeconds: Long = 3,
    ): List<Triple<Commute, Accommodation, Attraction?>> {
        val commuteGroups = commutes.groupBy { it.arrival.location }
        val accommodationGroups = accommodations.groupBy { it.location }
        val attractionGroups = attractions.groupBy { it.location }

        return accommodationGroups.flatMap { (location, accommodations) ->
            val nearbyAttractions = attractionGroups[location] ?: emptyList()
            val nearbyCommutes = commuteGroups[location] ?: emptyList()

            accommodations.flatMap { accommodation ->
                val validAttractions = nearbyAttractions.filter { it.date < accommodation.rent.till && it.date > accommodation.rent.from }
                val validCommutes =
                    nearbyCommutes.filter {
                        it.arrival.time.isBefore(accommodation.rent.from) &&
                            Duration.between(it.arrival.time, accommodation.rent.from).seconds <= creationWindowSeconds
                    }

                validCommutes.flatMap { commute ->
                    val basicOffer = Triple(commute, accommodation, null)

                    val offersWithAttractions =
                        validAttractions.map { attraction ->
                            Triple(commute, accommodation, attraction)
                        }

                    listOf(basicOffer) + offersWithAttractions
                }
            }
        }
    }

    private fun Triple<Commute, Accommodation, Attraction?>.toCommand(): CreateTravelOfferCommand {
        val (commute, accommodation, attraction) = this
        return CreateTravelOfferCommand(
            travelOfferId = UUID.randomUUID(),
            correlationId = UUID.randomUUID(),
            name = "${commute.name} ${accommodation.name}${attraction?.name?.let { " $it" } ?: ""}",
            commuteId = commute.id,
            accommodationId = accommodation.id,
            attractionId = attraction?.id,
        )
    }

    private fun Triple<Commute, Accommodation, Attraction?>.toIds(): Triple<UUID, UUID, UUID?> {
        val (commute, accommodation, attraction) = this
        return Triple(commute.id, accommodation.id, attraction?.id)
    }
}
