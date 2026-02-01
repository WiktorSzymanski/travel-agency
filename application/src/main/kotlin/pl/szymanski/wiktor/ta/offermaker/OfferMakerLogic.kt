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
import java.util.UUID

class OfferMakerLogic(
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

        matchWithCommute(commute, envelope.metadata.correlationId)
    }

    suspend fun onAccommodationCreatedEvent(envelope: EventEnvelope<AccommodationCreatedEvent>) {
        val accommodation = Accommodation.fromEvents(listOf(envelope.event))

        if (accommodation.rent.from.isBefore(LocalDateTime.now()))
            return

        matchWithAccommodation(accommodation, envelope.metadata.correlationId)
    }

    suspend fun onAttractionCreatedEvent(envelope: EventEnvelope<AttractionCreatedEvent>) {
        val attraction = Attraction.fromEvents(listOf(envelope.event))

        if (attraction.date.isBefore(LocalDateTime.now()))
            return

        matchWithAttraction(attraction, envelope.metadata.correlationId)
    }

    private suspend fun matchWithCommute(newCommute: Commute, correlationId: UUID) {
        val matches = accommodationQueryRepository.findByLocationAndDate(
            newCommute.arrival.location,
            LocalDateTimeRange(
                newCommute.arrival.time,
                newCommute.arrival.time.plusSeconds(creationWindowSeconds)
            )
        )

        matches.forEach { accommodation ->
            createOffers(newCommute, accommodation, correlationId)
        }
    }

    private suspend fun matchWithAccommodation(newAccommodation: Accommodation, correlationId: UUID) {
        val matchingCommutes = commuteQueryRepository.findByLocationAndArrivalDate(
            newAccommodation.location,
            LocalDateTimeRange(
                from = newAccommodation.rent.from.minusSeconds(creationWindowSeconds),
                till = newAccommodation.rent.from
            )
        )

        matchingCommutes.forEach { commute ->
            createOffers(commute, newAccommodation, correlationId)
        }
    }

    private suspend fun matchWithAttraction(newAttraction: Attraction, correlationId: UUID) {
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

    private suspend fun createOffers(commute: Commute, accommodation: Accommodation, correlationId: UUID) {
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
        /** It needs index of all 3 ids to be unique **/
        travelOfferQueryRepository.save(TravelOffer(commute.id, accommodation.id, attraction?.id ?: AttractionId.Empty))
    }
}
