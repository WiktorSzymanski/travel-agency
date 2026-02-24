package pl.szymanski.wiktor.ta.infrastructure.dto

import kotlinx.serialization.Serializable
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import java.util.UUID

@Serializable
data class CreateBookingRequest(
    val travelOffer: TravelOfferDto,
    val userId: String,
    val seat: SeatDto,
) {
    fun userIdAsUUID(): UUID = UUID.fromString(userId)

    fun travelOfferToDomain(): TravelOffer = travelOffer.toDomain()
}




