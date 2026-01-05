package pl.szymanski.wiktor.ta.offermaker

import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.AccommodationId
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.aggregate.CommuteId
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

    fun addCommute(commute: Commute) = repository.saveCommute(commute)
    fun addAccommodation(accommodation: Accommodation) = repository.saveAccommodation(accommodation)
    fun addAttraction(attraction: Attraction) = repository.saveAttraction(attraction)

    fun addOfferTripleIfUnique(triple: Triple<CommuteId, AccommodationId, AttractionId>): Boolean =
        repository.addOfferTripleIfUnique(triple)
}