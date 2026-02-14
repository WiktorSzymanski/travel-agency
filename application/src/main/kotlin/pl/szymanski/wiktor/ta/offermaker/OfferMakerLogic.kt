package pl.szymanski.wiktor.ta.offermaker

import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import pl.szymanski.wiktor.ta.domain.event.AccommodationCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteCreatedEvent
import pl.szymanski.wiktor.ta.queryrepository.AccommodationQueryRepository
import pl.szymanski.wiktor.ta.queryrepository.AttractionQueryRepository
import pl.szymanski.wiktor.ta.queryrepository.CommuteQueryRepository
import pl.szymanski.wiktor.ta.queryrepository.TravelOfferQueryRepository
import java.time.LocalDateTime

open class OfferMakerLogic(
    private val travelOfferQueryRepository: TravelOfferQueryRepository,
    private val commuteQueryRepository: CommuteQueryRepository,
    private val accommodationQueryRepository: AccommodationQueryRepository,
    private val attractionQueryRepository: AttractionQueryRepository,
    private val creationWindowSeconds: Long = 3
) {

    suspend fun onCommuteCreatedEvent(envelope: EventEnvelope<CommuteCreatedEvent>) {
        val commute = Commute.fromEvents(listOf(envelope.event))

        if (commute.departure.time.isBefore(LocalDateTime.now()))
            return

        matchWithCommute(commute)
    }

    suspend fun onAccommodationCreatedEvent(envelope: EventEnvelope<AccommodationCreatedEvent>) {
        val accommodation = Accommodation.fromEvents(listOf(envelope.event))

        if (accommodation.rent.from.isBefore(LocalDateTime.now()))
            return

        matchWithAccommodation(accommodation)
    }

    suspend fun onAttractionCreatedEvent(envelope: EventEnvelope<AttractionCreatedEvent>) {
        val attraction = Attraction.fromEvents(listOf(envelope.event))

        if (attraction.date.isBefore(LocalDateTime.now()))
            return

        matchWithAttraction(attraction)
    }

    private suspend fun matchWithCommute(newCommute: Commute) {
        val matches = accommodationQueryRepository.findByLocationAndDate(
            newCommute.arrival.location,
            LocalDateTimeRange(
                newCommute.arrival.time,
                newCommute.arrival.time.plusSeconds(creationWindowSeconds)
            )
        )

        matches.forEach { accommodation ->
            createOffers(newCommute, accommodation)
        }
    }

    private suspend fun matchWithAccommodation(newAccommodation: Accommodation) {
        val matchingCommutes = commuteQueryRepository.findByLocationAndArrivalDate(
            newAccommodation.location,
            LocalDateTimeRange(
                from = newAccommodation.rent.from.minusSeconds(creationWindowSeconds),
                till = newAccommodation.rent.from
            )
        )

        matchingCommutes.forEach { commute ->
            createOffers(commute, newAccommodation)
        }
    }

    private suspend fun matchWithAttraction(newAttraction: Attraction) {
        val matchedAccommodations = accommodationQueryRepository.findByLocationAndRentContainsDate(
            newAttraction.location,
            newAttraction.date
        )

        if (matchedAccommodations.isEmpty()) return

        matchedAccommodations.forEach { accommodation ->
            val matchingCommutes = commuteQueryRepository.findByLocationAndArrivalDate(
                newAttraction.location,
                LocalDateTimeRange(
                    from = accommodation.rent.from.minusSeconds(creationWindowSeconds),
                    till = accommodation.rent.from
                )
            )
            matchingCommutes.forEach { commute ->
                saveTravelOffer(
                    commute,
                    accommodation,
                    newAttraction
                )
            }
        }
    }

    private suspend fun createOffers(commute: Commute, accommodation: Accommodation) {
        saveTravelOffer(commute, accommodation, null)

        val matchedAttractions = attractionQueryRepository.findByLocationAndDate(
            accommodation.location,
            LocalDateTimeRange(
                accommodation.rent.from,
                accommodation.rent.till
            )
        )

        matchedAttractions.forEach { attraction ->
            saveTravelOffer(
                commute,
                accommodation,
                attraction
            )
        }
    }

    private suspend fun saveTravelOffer(
        commute: Commute,
        accommodation: Accommodation,
        attraction: Attraction?,
    ) {
        /** It needs an index of all 3 ids to be unique **/
        travelOfferQueryRepository.save(TravelOffer(commute.id, accommodation.id, attraction?.id ?: AttractionId.Empty))
    }
}
