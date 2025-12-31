package pl.szymanski.wiktor.ta.offermaker

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import pl.szymanski.wiktor.ta.CommandBus
import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.command.CreateTravelOfferCommand
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.AccommodationId
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.aggregate.CommuteId
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOfferId
import pl.szymanski.wiktor.ta.domain.event.AccommodationCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteCreatedEvent
import pl.szymanski.wiktor.ta.launchCatching
import pl.szymanski.wiktor.ta.subscribe
import java.time.LocalDateTime
import java.util.UUID

class OfferMaker (
    private val eventBus: EventBus,
    private val commandBus: CommandBus,
    private val resourceService: ActiveResourceService,
    private val creationWindowSeconds: Long = 3,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {

    init {
        setupSubscriptions()
    }

    private fun setupSubscriptions() {
        scope.launch {
            eventBus.subscribe<CommuteCreatedEvent> { eventEnvelope ->
                val event = eventEnvelope.event

                if (event.departure.time.isBefore(LocalDateTime.now()))
                    return@subscribe

                resourceService.addCommute(event)

                // CHECK: shouldn't this whole code in subscribe be run in different thread
                scope.launchCatching {
                    matchWithCommute(event, eventEnvelope.metadata.correlationId)
                }
            }
        }

        scope.launch {
            eventBus.subscribe<AccommodationCreatedEvent> { eventEnvelope ->
                val event = eventEnvelope.event

                if (event.rent.from.isBefore(LocalDateTime.now()))
                    return@subscribe

                resourceService.addAccommodation(event)

                scope.launchCatching {
                    matchWithAccommodation(event, eventEnvelope.metadata.correlationId)
                }
            }
        }

        scope.launch {
            eventBus.subscribe<AttractionCreatedEvent> { eventEnvelope ->
                val event = eventEnvelope.event

                if (event.date.isBefore(LocalDateTime.now()))
                    return@subscribe

                resourceService.addAttraction(event)

                scope.launchCatching {
                    matchWithAttraction(event, eventEnvelope.metadata.correlationId)
                }
            }
        }
    }

    private suspend fun matchWithCommute(newCommute: CommuteCreatedEvent, correlationId: UUID) {
        val matches = resourceService.getAccommodations(
            location = newCommute.arrival.location,
            rentFrom = LocalDateTimeRange(
                from = newCommute.arrival.time,
                till = newCommute.arrival.time.plusSeconds(creationWindowSeconds)
            )
        )

        matches.forEach { accommodation ->
            createOffers(newCommute, accommodation, correlationId)
        }
    }

    private suspend fun matchWithAccommodation(newAccommodation: AccommodationCreatedEvent, correlationId: UUID) {
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

    private suspend fun matchWithAttraction(newAttraction: AttractionCreatedEvent, correlationId: UUID) {
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
                    commuteId = commute.id,
                    commuteName = commute.name,
                    accommodationId = accommodation.id,
                    accommodationName = accommodation.name,
                    attractionId = newAttraction.attractionId,
                    attractionName = newAttraction.name,
                    correlationId = correlationId
                )
            }
        }
    }

    private suspend fun createOffers(commute: CommuteCreatedEvent, accommodation: Accommodation, correlationId: UUID) {
        createOffers(
            commute.commuteId, commute.name,
            accommodation.id, accommodation.name, accommodation.location, accommodation.rent.from, accommodation.rent.till,
            correlationId
        )
    }

    private suspend fun createOffers(commute: Commute, accommodation: AccommodationCreatedEvent, correlationId: UUID) {
        createOffers(
            commute.id, commute.name,
            accommodation.accommodationId, accommodation.name, accommodation.location, accommodation.rent.from, accommodation.rent.till,
            correlationId
        )
    }

    private suspend fun createOffers(
        commuteId: CommuteId,
        commuteName: String,
        accommodationId: AccommodationId,
        accommodationName: String,
        accommodationLocation: LocationEnum,
        accommodationRentFrom: LocalDateTime,
        accommodationRentTill: LocalDateTime,
        correlationId: UUID
    ) {
        // Create basic offer (no attraction)
        dispatchCreateOffer(commuteId, commuteName, accommodationId, accommodationName, AttractionId.Empty, null, correlationId)

        // Find matching attractions for this pair
        val matchedAttractions = resourceService.getAttractions(
            location = accommodationLocation,
            date = LocalDateTimeRange(
                from = accommodationRentFrom,
                till = accommodationRentTill
            )
        )

        matchedAttractions.forEach { attraction ->
            dispatchCreateOffer(
                commuteId,
                commuteName,
                accommodationId,
                accommodationName,
                attraction.id,
                attraction.name,
                correlationId
            )
        }
    }

    private suspend fun dispatchCreateOffer(
        commuteId: CommuteId,
        commuteName: String,
        accommodationId: AccommodationId,
        accommodationName: String,
        attractionId: AttractionId,
        attractionName: String?,
        correlationId: UUID
    ) {
        val triple = Triple(commuteId, accommodationId, attractionId)

        if (resourceService.addOfferTripleIfUnique(triple)) {
            val command = CreateTravelOfferCommand(
                travelOfferId = TravelOfferId.from(UUID.randomUUID()), // TODO: What to do with those, Id in command is meaning less, aggregate upon creating creates its own
                correlationId = correlationId,
                name = "$commuteName $accommodationName${attractionName?.let { " $it" } ?: ""}",
                commuteId = commuteId,
                accommodationId = accommodationId,
                attractionId = attractionId
            )
            commandBus.dispatchAndForget(command)
        }
    }
}