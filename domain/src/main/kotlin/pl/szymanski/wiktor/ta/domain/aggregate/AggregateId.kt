package pl.szymanski.wiktor.ta.domain.aggregate

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
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

@Serializable(with = AttractionIdSerializer::class)
sealed interface AttractionId {
    @Serializable
    val value: UUID?

    @Serializable
    data object Empty : AttractionId {
        override val value: UUID? = null
    }

    @Serializable
    @JvmInline
    value class Present(@Serializable(with = UUIDSerializer::class) override val value: UUID) : AttractionId, TravelOfferComponentId

    companion object {
        fun generate(): Present = Present(UUID.randomUUID())
        fun from(value: UUID?): AttractionId = value?.let { Present(it) } ?: Empty
    }
}

object AttractionIdSerializer : KSerializer<AttractionId> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("AttractionId", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: AttractionId) {
        when (value) {
            is AttractionId.Empty -> encoder.encodeString("EMPTY")
            is AttractionId.Present -> encoder.encodeString(value.value.toString())
        }
    }

    override fun deserialize(decoder: Decoder): AttractionId {
        val s = decoder.decodeString()
        return if (s == "EMPTY") AttractionId.Empty else AttractionId.Present(UUID.fromString(s))
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

@Serializable(with = BookingIdSerializer::class)
sealed interface BookingId {
    @Serializable
    val value: UUID?

    @Serializable
    data object Empty : BookingId {
        override val value: UUID? = null
    }

    @Serializable
    @JvmInline
    value class Present(@Serializable(with = UUIDSerializer::class) override val value: UUID) : BookingId, AggregateId

    companion object {
        fun generate(): Present = Present(UUID.randomUUID())
        fun from(value: UUID?): BookingId = value?.let { Present(it) } ?: Empty
    }
}

object BookingIdSerializer : KSerializer<BookingId> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BookingId", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: BookingId) {
        when (value) {
            is BookingId.Empty -> encoder.encodeString("EMPTY")
            is BookingId.Present -> encoder.encodeString(value.value.toString())
        }
    }

    override fun deserialize(decoder: Decoder): BookingId {
        val s = decoder.decodeString()
        return if (s == "EMPTY") BookingId.Empty else BookingId.Present(UUID.fromString(s))
    }
}
