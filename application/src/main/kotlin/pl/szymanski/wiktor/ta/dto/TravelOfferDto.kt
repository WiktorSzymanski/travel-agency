package pl.szymanski.wiktor.ta.dto

import kotlinx.serialization.Serializable
import pl.szymanski.wiktor.ta.domain.TravelOfferStatusEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer

// TODO: Current DTO's are views not DTO's that will be saved in database

@Serializable
data class TravelOfferDto(
    val id: String,
    val name: String,
    val commute: CommuteDto,
    val accommodation: AccommodationDto,
    val attraction: AttractionDto,
    val booking: String,
    val status: String = TravelOfferStatusEnum.AVAILABLE.name,
) {
    companion object {
        fun fromDomain(
            travelOffer: TravelOffer,
            commute: Commute,
            accommodation: Accommodation,
            attraction: Attraction?,
        ): TravelOfferDto {
            return TravelOfferDto(
                id = travelOffer.id.toString(),
                name = travelOffer.name,
                commute = CommuteDto.fromDomain(commute),
                accommodation = AccommodationDto.fromDomain(accommodation),
                attraction = AttractionDto.fromDomain(attraction),
                booking = travelOffer.bookingId.toString(),
                status = travelOffer.status.name,
            )
        }
    }
}
