package pl.szymanski.wiktor.ta.domain.aggregate

import kotlinx.serialization.Serializable
import pl.szymanski.wiktor.ta.domain.UUIDSerializer
import java.util.UUID

@Serializable
sealed interface AggregateId {
    @Serializable(with = UUIDSerializer::class)
    val value: UUID
}

@Serializable
sealed interface TravelOfferComponentId: AggregateId

@Serializable
@JvmInline
value class AccommodationId(@Serializable(with = UUIDSerializer::class) override val value: UUID) : TravelOfferComponentId {
    companion object {
        fun generate(): AccommodationId = AccommodationId(UUID.randomUUID())
        fun from(value: UUID): AccommodationId = AccommodationId(value)
    }
}

@Serializable
sealed interface AttractionId {
    @Serializable
    data object Empty : AttractionId

    @Serializable
    @JvmInline
    value class Present(@Serializable(with = UUIDSerializer::class) override val value: UUID) : AttractionId, TravelOfferComponentId

    companion object {
        fun generate(): Present = Present(UUID.randomUUID())
        fun from(value: UUID?): AttractionId = value?.let { Present(it) } ?: Empty
    }
}

@Serializable
@JvmInline
value class CommuteId(@Serializable(with = UUIDSerializer::class) override val value: UUID) : TravelOfferComponentId {
    companion object {
        fun generate(): CommuteId = CommuteId(UUID.randomUUID())
        fun from(value: UUID): CommuteId = CommuteId(value)
    }
}

@Serializable
sealed interface BookingId {
    @Serializable
    data object Empty : BookingId

    @Serializable
    @JvmInline
    value class Present(@Serializable(with = UUIDSerializer::class) override val value: UUID) : BookingId, AggregateId

    companion object {
        fun generate(): Present = Present(UUID.randomUUID())
    }
}
