package pl.szymanski.wiktor.ta.offermaker

import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.event.AccommodationCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteCreatedEvent
import java.util.UUID

class ActiveResourceService(private val repository: ActiveResourceRepository) {
    fun getCommutes(location: LocationEnum? = null, arrival: LocalDateTimeRange? = null): List<Commute> = repository.getCommutes(location, arrival)
    fun getAccommodations(
        location: LocationEnum? = null,
        rentFrom: LocalDateTimeRange? = null,
        rentTill: LocalDateTimeRange? = null,
    ): List<Accommodation> = repository.getAccommodations(location, rentFrom, rentTill)
    fun getAttractions(location: LocationEnum? = null, date: LocalDateTimeRange? = null): List<Attraction> = repository.getAttractions(location, date)

    fun addCommute(event: CommuteCreatedEvent) = repository.saveCommute(Commute.fromEvents(listOf(event)))
    fun addAccommodation(event: AccommodationCreatedEvent) = repository.saveAccommodation(Accommodation.fromEvents(listOf(event)))
    fun addAttraction(event: AttractionCreatedEvent) = repository.saveAttraction(Attraction.fromEvents(listOf(event)))

    fun addOfferTripleIfUnique(triple: Triple<UUID, UUID, UUID?>): Boolean =
        repository.addOfferTripleIfUnique(triple)
}