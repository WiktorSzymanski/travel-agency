package pl.szymanski.wiktor.ta.infrastructure.dto

import kotlinx.serialization.Serializable
import pl.szymanski.wiktor.ta.domain.TravelOfferStatusEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer

@Serializable
data class TravelOfferDto(
    val commuteId: String,
    val accommodationId: String,
    val attractionId: String,
) {
    companion object {
        fun fromDomain(travelOffer: TravelOffer): TravelOfferDto {
            return TravelOfferDto(
                commuteId = travelOffer.commuteId.toString(),
                accommodationId = travelOffer.accommodationId.toString(),
                attractionId = travelOffer.attractionId.toString(),
            )
        }
    }
}
