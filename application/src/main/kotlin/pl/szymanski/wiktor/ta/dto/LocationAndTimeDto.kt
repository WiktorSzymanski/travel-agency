package pl.szymanski.wiktor.ta.dto

import kotlinx.serialization.Serializable
import pl.szymanski.wiktor.ta.domain.LocationAndTime

@Serializable
data class LocationAndTimeDto(
    val location: String,
    val time: String,
) {
    companion object {
        fun fromDomain(locationAndTime: LocationAndTime) =
            LocationAndTimeDto(
                location = locationAndTime.location.name,
                time = locationAndTime.time.toString(),
            )
    }
}
