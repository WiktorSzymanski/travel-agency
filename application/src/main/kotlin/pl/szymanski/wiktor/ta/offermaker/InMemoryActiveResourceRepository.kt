package pl.szymanski.wiktor.ta.offermaker

import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class InMemoryActiveResourceRepository : ActiveResourceRepository {
    private val commutes = ConcurrentHashMap<UUID, Commute>()
    private val accommodations = ConcurrentHashMap<UUID, Accommodation>()
    private val attractions = ConcurrentHashMap<UUID, Attraction>()
    private val offerTriples = Collections.synchronizedSet(mutableSetOf<Triple<UUID, UUID, UUID?>>())

    override fun getCommutes(location: LocationEnum?, arrival: LocalDateTimeRange?): List<Commute> {
        val now = LocalDateTime.now()
        commutes.values.removeIf { it.departure.time.isBefore(now) }
        return commutes.values.filter {
            (location == null || it.arrival.location == location) &&
            (arrival == null || (arrival.from == null || !it.arrival.time.isBefore(arrival.from)) && (arrival.till == null || !it.arrival.time.isAfter(arrival.till)))
        }
    }

    override fun getAccommodations(
        location: LocationEnum?,
        rentFrom: LocalDateTimeRange?,
        rentTill: LocalDateTimeRange?,
    ): List<Accommodation> {
        val now = LocalDateTime.now()
        accommodations.values.removeIf { it.rent.from.isBefore(now) }
        return accommodations.values.filter {
            (location == null || it.location == location) &&
            (rentFrom == null || (rentFrom.from == null || !it.rent.from.isBefore(rentFrom.from)) && (rentFrom.till == null || !it.rent.from.isAfter(rentFrom.till))) &&
            (rentTill == null || (rentTill.from == null || !it.rent.till.isBefore(rentTill.from)) && (rentTill.till == null || !it.rent.till.isAfter(rentTill.till)))
        }
    }

    override fun getAttractions(location: LocationEnum?, date: LocalDateTimeRange?): List<Attraction> {
        val now = LocalDateTime.now()
        attractions.values.removeIf { it.date.isBefore(now) }
        return attractions.values.filter {
            (location == null || it.location == location) &&
            (date == null || (date.from == null || !it.date.isBefore(date.from)) && (date.till == null || !it.date.isAfter(date.till)))
        }
    }

    override fun saveCommute(commute: Commute) {
        commutes[commute.id] = commute
    }

    override fun saveAccommodation(accommodation: Accommodation) {
        accommodations[accommodation.id] = accommodation
    }

    override fun saveAttraction(attraction: Attraction) {
        attractions[attraction.id] = attraction
    }

    override fun removeCommute(id: UUID) {
        commutes.remove(id)
    }

    override fun removeAccommodation(id: UUID) {
        accommodations.remove(id)
    }

    override fun removeAttraction(id: UUID) {
        attractions.remove(id)
    }

    override fun getLastCreatedOffersTriples(): List<Triple<UUID, UUID, UUID?>> = offerTriples.toList()

    override fun addOfferTripleIfUnique(triple: Triple<UUID, UUID, UUID?>): Boolean =
        offerTriples.add(triple)
}
