package pl.szymanski.wiktor.ta.domain.aggregate

import java.util.UUID

sealed interface AggregateId {
    val value: UUID
}

sealed interface TravelOfferComponentId: AggregateId

@JvmInline
value class AccommodationId(override val value: UUID) : TravelOfferComponentId {
    companion object {
        fun generate(): AccommodationId = AccommodationId(UUID.randomUUID())
        fun from(value: UUID): AccommodationId = AccommodationId(value)
    }
}

sealed interface AttractionId {
    data object Empty : AttractionId

    @JvmInline
    value class Present(override val value: UUID) : AttractionId, TravelOfferComponentId

    companion object {
        fun generate(): Present = Present(UUID.randomUUID())
        fun from(value: UUID?): AttractionId = value?.let { Present(it) } ?: Empty
    }
}

@JvmInline
value class CommuteId(override val value: UUID) : TravelOfferComponentId {
    companion object {
        fun generate(): CommuteId = CommuteId(UUID.randomUUID())
        fun from(value: UUID): CommuteId = CommuteId(value)
    }
}

@JvmInline
value class TravelOfferId(override val value: UUID) : AggregateId {
    companion object {
        fun generate(commuteId: CommuteId, accommodationId: AccommodationId, attractionId: AttractionId): TravelOfferId =
            TravelOfferId(
                UUID.nameUUIDFromBytes(
                    "${commuteId}${accommodationId}${attractionId}".toByteArray(Charsets.UTF_8)
                )
            )
        fun from(value: UUID): TravelOfferId = TravelOfferId(value)
    }
}

sealed interface BookingId {
    data object Empty : BookingId

    @JvmInline
    value class Present(override val value: UUID) : BookingId, AggregateId

    companion object {
        fun generate(): Present = Present(UUID.randomUUID())
    }
}