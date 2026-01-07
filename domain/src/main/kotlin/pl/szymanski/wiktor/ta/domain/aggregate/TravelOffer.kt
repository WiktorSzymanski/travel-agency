package pl.szymanski.wiktor.ta.domain.aggregate

data class TravelOffer(
    val commuteId: CommuteId,
    val accommodationId: AccommodationId,
    val attractionId: AttractionId = AttractionId.Empty
)
