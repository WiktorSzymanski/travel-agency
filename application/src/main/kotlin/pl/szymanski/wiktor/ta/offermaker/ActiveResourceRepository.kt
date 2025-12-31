package pl.szymanski.wiktor.ta.offermaker

import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.AccommodationId
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.aggregate.CommuteId
import java.util.UUID

interface ActiveResourceRepository {
    fun getCommutes(location: LocationEnum? = null, arrival: LocalDateTimeRange? = null): List<Commute>
    fun getAccommodations(
        location: LocationEnum? = null,
        rentFrom: LocalDateTimeRange? = null,
        rentTill: LocalDateTimeRange? = null,
    ): List<Accommodation>
    fun getAttractions(location: LocationEnum? = null, date: LocalDateTimeRange? = null): List<Attraction>

    fun saveCommute(commute: Commute)
    fun saveAccommodation(accommodation: Accommodation)
    fun saveAttraction(attraction: Attraction)

    fun removeCommute(id: UUID)
    fun removeAccommodation(id: UUID)
    fun removeAttraction(id: UUID)

    fun getLastCreatedOffersTriples(): List<Triple<CommuteId, AccommodationId, AttractionId>>
    fun addOfferTripleIfUnique(triple: Triple<CommuteId, AccommodationId, AttractionId>): Boolean
}