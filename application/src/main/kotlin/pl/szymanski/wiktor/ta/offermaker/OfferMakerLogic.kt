package pl.szymanski.wiktor.ta.offermaker

import pl.szymanski.wiktor.ta.CommandBus
import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.command.CreateTravelOfferCommand
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.AccommodationId
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.aggregate.CommuteId
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOfferId
import pl.szymanski.wiktor.ta.domain.event.AccommodationCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteCreatedEvent
import java.time.LocalDateTime
import java.util.UUID

class OfferMakerLogic(
    private val commandBus: CommandBus,
    private val resourceService: ActiveResourceService,
    private val creationWindowSeconds: Long = 3
) {

    suspend fun onCommuteCreatedEvent(envelope: EventEnvelope<CommuteCreatedEvent>) {
        val commute = Commute.fromEvents(listOf(envelope.event))

        if (commute.departure.time.isBefore(LocalDateTime.now()))
            return

        resourceService.addCommute(commute)

        matchWithCommute(commute, envelope.metadata.correlationId)
    }

    suspend fun onAccommodationCreatedEvent(envelope: EventEnvelope<AccommodationCreatedEvent>) {
        val accommodation = Accommodation.fromEvents(listOf(envelope.event))

        if (accommodation.rent.from.isBefore(LocalDateTime.now()))
            return

        resourceService.addAccommodation(accommodation)

        matchWithAccommodation(accommodation, envelope.metadata.correlationId)
    }

    suspend fun onAttractionCreatedEvent(envelope: EventEnvelope<AttractionCreatedEvent>) {
        val attraction = Attraction.fromEvents(listOf(envelope.event))

        if (attraction.date.isBefore(LocalDateTime.now()))
            return

        resourceService.addAttraction(attraction)

        matchWithAttraction(attraction, envelope.metadata.correlationId)
    }

    private suspend fun matchWithCommute(newCommute: Commute, correlationId: UUID) {
        val matches = resourceService.getAccommodations(
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
        val matchingCommutes = resourceService.getCommutes(
            location = newAccommodation.location,
            arrival = LocalDateTimeRange(
                from = newAccommodation.rent.from.minusSeconds(creationWindowSeconds),
                till = newAccommodation.rent.from
            )
        )

        matchingCommutes.forEach { commute ->
            createOffers(commute, newAccommodation, correlationId)
        }
    }

    private suspend fun matchWithAttraction(newAttraction: Attraction, correlationId: UUID) {
        val matchedAccommodations = resourceService.getAccommodations(
            location = newAttraction.location,
            rentFrom = LocalDateTimeRange(till = newAttraction.date),
            rentTill = LocalDateTimeRange(from = newAttraction.date)
        )

        if (matchedAccommodations.isEmpty()) return

        matchedAccommodations.forEach { accommodation ->
            val matchingCommutes = resourceService.getCommutes(
                location = newAttraction.location,
                arrival = LocalDateTimeRange(
                    from = accommodation.rent.from.minusSeconds(creationWindowSeconds),
                    till = accommodation.rent.from
                )
            )
            matchingCommutes.forEach { commute ->
                dispatchCreateOffer(
                    commute,
                    accommodation,
                    newAttraction,
                    correlationId
                )
            }
        }
    }

    private suspend fun createOffers(commute: Commute, accommodation: Accommodation, correlationId: UUID) {
        dispatchCreateOffer(commute, accommodation, null, correlationId)

        val matchedAttractions = resourceService.getAttractions(
            accommodation.location,
            LocalDateTimeRange(
                accommodation.rent.from,
                accommodation.rent.till
            )
        )

        matchedAttractions.forEach { attraction ->
            dispatchCreateOffer(
                commute,
                accommodation,
                attraction,
                correlationId
            )
        }
    }

    private suspend fun dispatchCreateOffer(
        commute: Commute,
        accommodation: Accommodation,
        attraction: Attraction?,
        correlationId: UUID
    ) {
        val triple = Triple(commute.id, accommodation.id, attraction?.id ?: AttractionId.Empty)

        if (resourceService.addOfferTripleIfUnique(triple)) {
            val command = CreateTravelOfferCommand(
                travelOfferId = TravelOfferId.from(UUID.randomUUID()),
                correlationId = correlationId,
                name = "${commute.name} ${accommodation.name} ${attraction?.let { " ${it.name}" } ?: ""}",
                commuteId = commute.id,
                accommodationId = accommodation.id,
                attractionId = attraction?.id ?: AttractionId.Empty
            )
            commandBus.dispatchAndForget(command)
        }
    }
}
