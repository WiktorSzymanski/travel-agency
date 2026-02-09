package pl.szymanski.wiktor.ta.domain.aggregate

import kotlinx.serialization.Serializable

@Serializable
data class TravelOffer(
    val commuteId: CommuteId,
    val accommodationId: AccommodationId,
    val attractionId: AttractionId = AttractionId.Empty
)
