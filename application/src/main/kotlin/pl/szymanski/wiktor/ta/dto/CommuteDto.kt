package pl.szymanski.wiktor.ta.dto

import kotlinx.serialization.Serializable
import pl.szymanski.wiktor.ta.domain.aggregate.Commute

@Serializable
data class CommuteDto(
    val id: String,
    val name: String,
    val departure: LocationAndTimeDto,
    val arrival: LocationAndTimeDto,
    val availableSeats: List<String>,
) {
    companion object {
        fun fromDomain(commute: Commute) =
            CommuteDto(
                id = commute._id.toString(),
                name = commute.name,
                departure = LocationAndTimeDto.fromDomain(commute.departure),
                arrival = LocationAndTimeDto.fromDomain(commute.arrival),
                availableSeats = commute.seats.map { it.toString() }.filter { !commute.bookings.containsKey(it) },
            )
    }
}
