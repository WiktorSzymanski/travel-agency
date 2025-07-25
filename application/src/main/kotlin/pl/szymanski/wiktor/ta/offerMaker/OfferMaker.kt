package pl.szymanski.wiktor.ta.offerMaker

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.command.CreateTravelOfferCommand
import pl.szymanski.wiktor.ta.commandHandler.TravelOfferCommandHandler
import pl.szymanski.wiktor.ta.domain.AccommodationStatusEnum
import pl.szymanski.wiktor.ta.domain.AttractionStatusEnum
import pl.szymanski.wiktor.ta.domain.CommuteStatusEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.event.TravelOfferExpiredEvent
import pl.szymanski.wiktor.ta.domain.repository.AccommodationRepository
import pl.szymanski.wiktor.ta.domain.repository.AttractionRepository
import pl.szymanski.wiktor.ta.domain.repository.CommuteRepository
import pl.szymanski.wiktor.ta.timeMet
import java.time.temporal.ChronoUnit
import java.util.UUID

class OfferMaker(
    private val accommodationRepository: AccommodationRepository,
    private val attractionRepository: AttractionRepository,
    private val commuteRepository: CommuteRepository,
    private val travelOfferCommandHandler: TravelOfferCommandHandler,
) {
    private val offerHashes = mutableListOf<Int>()

    init {
        popExpiredHashes()
    }

    private fun popExpiredHashes(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) {
        scope.launch {
            EventBus.subscribe<TravelOfferExpiredEvent> {
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
                            try {
                                travelOfferCommandHandler.handle(offerTriple.toCommand())
                            } catch (e: Throwable) {
                                println("ERROR HANDLED: $e")
                            }
                        }
                        offerHashes.add(offerMatchHash)
                    }
                }
            }
        }

    private suspend fun collectData() =
        coroutineScope {
            val accommodations = async { accommodationRepository.findAllByStatus(AccommodationStatusEnum.AVAILABLE ) }
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
    ): List<Triple<Commute, Accommodation, Attraction?>> {
        val commuteGroups = commutes.groupBy { it.arrival.location }
        val accommodationGroups = accommodations.groupBy { it.location }
        val attractionGroups = attractions.groupBy { it.location }

        return accommodationGroups.flatMap { (location, accommodations) ->
            val nearbyAttractions = attractionGroups[location] ?: emptyList()
            val nearbyCommutes = commuteGroups[location] ?: emptyList()

            accommodations.flatMap { accommodation ->
                val validAttractions = nearbyAttractions.filter { it.date < accommodation.rent.till }
                val validCommutes =
                    nearbyCommutes.filter {
                        it.arrival.time.truncatedTo(ChronoUnit.MINUTES) ==
                            accommodation.rent.from.truncatedTo(ChronoUnit.MINUTES)
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
            commuteId = commute._id,
            accommodationId = accommodation._id,
            attractionId = attraction?._id,
        )
    }

    private fun Triple<Commute, Accommodation, Attraction?>.toIds(): Triple<UUID, UUID, UUID?> {
        val (commute, accommodation, attraction) = this
        return Triple(commute._id, accommodation._id, attraction?._id)
    }
}
