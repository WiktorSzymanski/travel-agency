package pl.szymanski.wiktor.ta.dto

import kotlinx.serialization.Serializable
import pl.szymanski.wiktor.ta.domain.Rent

@Serializable
data class RentDto(
    val from: String,
    val till: String,
) {
    companion object {
        fun fromDomain(rent: Rent) =
            RentDto(
                from = rent.from.toString(),
                till = rent.till.toString(),
            )
    }
}
