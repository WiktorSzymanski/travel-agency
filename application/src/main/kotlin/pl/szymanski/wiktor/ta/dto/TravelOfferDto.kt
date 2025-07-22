package pl.szymanski.wiktor.ta.dto

import kotlinx.serialization.Serializable
import pl.szymanski.wiktor.ta.domain.OfferStatusEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer

@Serializable
data class TravelOfferDto(
    val id: String,
    val name: String,
    val commute: CommuteDto,
    val accommodation: AccommodationDto,
    val attraction: AttractionDto? = null,
    val booking: BookingDto? = null,
    val status: String = OfferStatusEnum.AVAILABLE.name,
) {
    companion object {
        fun fromDomain(
            travelOffer: TravelOffer,
            commute: Commute,
            accommodation: Accommodation,
            attraction: Attraction?,
        ): TravelOfferDto {
            return TravelOfferDto(
                id = travelOffer._id.toString(),
                name = travelOffer.name,
                commute = CommuteDto.fromDomain(commute),
                accommodation = AccommodationDto.fromDomain(accommodation),
                attraction = if (attraction != null) AttractionDto.fromDomain(attraction) else null,
                booking = travelOffer.booking?.let { BookingDto.fromDomain(it) },
                status = travelOffer.status.name,
            )
        }
    }
}