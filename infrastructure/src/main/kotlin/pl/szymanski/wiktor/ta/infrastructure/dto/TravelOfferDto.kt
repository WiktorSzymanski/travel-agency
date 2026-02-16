package pl.szymanski.wiktor.ta.infrastructure.dto

import kotlinx.serialization.Serializable
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import pl.szymanski.wiktor.ta.domain.aggregate.AccommodationId
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
import pl.szymanski.wiktor.ta.domain.aggregate.CommuteId
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import java.util.UUID

@Serializable
@CompoundIndexes(
    CompoundIndex(name = "idx_composite", def = "{'commuteId': 1, 'accommodationId': 1, 'attractionId': 1}")
)
data class TravelOfferDto(
    val commuteId: String,
    val accommodationId: String,
    val attractionId: String,
) {
    companion object {
        fun fromDomain(travelOffer: TravelOffer): TravelOfferDto {
            return TravelOfferDto(
                commuteId = travelOffer.commuteId.value.toString(),
                accommodationId = travelOffer.accommodationId.value.toString(),
                attractionId = travelOffer.attractionId.value?.toString() ?: "",
            )
        }
    }

    fun toDomain(): TravelOffer =
        TravelOffer(
            commuteId = CommuteId.from(UUID.fromString(commuteId)),
            accommodationId = AccommodationId.from(UUID.fromString(accommodationId)),
            attractionId = if (attractionId.isNotBlank() && attractionId != "Empty")
                AttractionId.from(UUID.fromString(attractionId))
            else
                AttractionId.Empty
        )
}
