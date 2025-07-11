package pl.szymanski.wiktor.ta.offerMaker

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import pl.szymanski.wiktor.ta.domain.AccommodationStatusEnum
import pl.szymanski.wiktor.ta.domain.AttractionStatusEnum
import pl.szymanski.wiktor.ta.domain.CommuteStatusEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import pl.szymanski.wiktor.ta.domain.repository.AccommodationRepository
import pl.szymanski.wiktor.ta.domain.repository.AttractionRepository
import pl.szymanski.wiktor.ta.domain.repository.CommuteRepository
import pl.szymanski.wiktor.ta.domain.repository.TravelOfferRepository
import pl.szymanski.wiktor.ta.timeMet
import java.time.temporal.ChronoUnit
import java.util.UUID

fun makeOffers(
    commutes: List<Commute>,
    accommodations: List<Accommodation>,
    attractions: List<Attraction>,
): List<TravelOffer> {
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
                val basicOffer =
                    TravelOffer(
                        UUID.randomUUID(),
                        "${commute.name} ${accommodation.name}",
                        commute._id,
                        accommodation._id,
                    )

                val offersWithAttractions =
                    validAttractions.map { attraction ->
                        TravelOffer(
                            UUID.randomUUID(),
                            "${commute.name} ${accommodation.name} ${attraction.name}",
                            commute._id,
                            accommodation._id,
                            attraction._id,
                        )
                    }

                listOf(basicOffer) + offersWithAttractions
            }
        }
    }
}

// maybe cache with hashes of entities combinations as "support" for db dup indexes?

suspend fun offerMaker(
    accommodationRepository: AccommodationRepository,
    attractionRepository: AttractionRepository,
    commuteRepository: CommuteRepository,
    travelOfferRepository: TravelOfferRepository,
) = coroutineScope {
    val accommodations =
        async {
            accommodationRepository.findAll()
                .filter { it.status == AccommodationStatusEnum.AVAILABLE }
        }
    val attractions = async { attractionRepository.findAll().filter { it.status == AttractionStatusEnum.SCHEDULED } }
    val commutes = async { commuteRepository.findAll().filter { it.status == CommuteStatusEnum.SCHEDULED } }

    val accommodationPairs = async { accommodations.await().partition { it.timeMet() } }
    val attractionPairs = async { attractions.await().partition { it.timeMet() } }
    val commutePairs = async { commutes.await().partition { it.timeMet() } }

    val (expiredAccommodations, validAccommodations) = accommodationPairs.await()
    val (expiredAttractions, validAttractions) = attractionPairs.await()
    val (expiredCommutes, validCommutes) = commutePairs.await()

    if (expiredAccommodations.isNotEmpty()) launch { accommodationRepository.updateAllStatus(expiredAccommodations) }
    if (expiredAttractions.isNotEmpty()) launch { attractionRepository.updateAllStatus(expiredAttractions) }
    if (expiredCommutes.isNotEmpty()) launch { commuteRepository.updateAllStatus(expiredCommutes) }

    val offers = makeOffers(validCommutes, validAccommodations, validAttractions)
    if (offers.isNotEmpty()) launch { travelOfferRepository.saveAll(offers) }
}
