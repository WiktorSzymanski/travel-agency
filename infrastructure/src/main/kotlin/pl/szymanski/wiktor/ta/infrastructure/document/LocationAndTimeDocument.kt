package pl.szymanski.wiktor.ta.infrastructure.document

import pl.szymanski.wiktor.ta.domain.LocationAndTime
import pl.szymanski.wiktor.ta.domain.LocationEnum
import java.time.LocalDateTime

data class LocationAndTimeDocument(
    val location: String,
    val time: String,
) {
    companion object {
        fun fromDomain(locationAndTime: LocationAndTime) =
            LocationAndTimeDocument(
                location = locationAndTime.location.name,
                time = locationAndTime.time.toString(),
            )
    }

    fun toDomain(): LocationAndTime =
        LocationAndTime(
            location = LocationEnum.valueOf(location),
            time = LocalDateTime.parse(time)
        )
}
