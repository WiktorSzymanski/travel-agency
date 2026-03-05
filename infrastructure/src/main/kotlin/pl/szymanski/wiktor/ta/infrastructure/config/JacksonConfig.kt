package pl.szymanski.wiktor.ta.infrastructure.config

import tools.jackson.core.JsonGenerator
import tools.jackson.core.JsonParser
import tools.jackson.core.JsonToken
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.JsonNode
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.deser.std.StdDeserializer
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.module.SimpleModule
import tools.jackson.databind.ser.std.StdSerializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import pl.szymanski.wiktor.ta.domain.AnySeat
import pl.szymanski.wiktor.ta.domain.PickedSeat
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.aggregate.AccommodationId
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import pl.szymanski.wiktor.ta.domain.aggregate.CommuteId
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import java.util.UUID

/**
 * Deserializes UUID from either:
 *  - a JSON string  "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"  (Jackson / new format)
 *  - a JSON array   ["UUID", "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"]  (kotlinx.serialization legacy format)
 */
class LegacyUUIDDeserializer : StdDeserializer<UUID>(UUID::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): UUID {
        return if (p.currentToken() == JsonToken.START_ARRAY) {
            p.nextToken() // first element: type discriminator string e.g. "UUID"
            p.nextToken() // second element: the actual UUID string
            val uuidString = p.getValueAsString()
            p.nextToken() // consume END_ARRAY
            UUID.fromString(uuidString)
        } else {
            UUID.fromString(p.getValueAsString())
        }
    }
}

/**
 * Deserializes Seat from either:
 *  - new format: {"type":"PickedSeat","row":"1","column":"A"} or {"type":"AnySeat"}
 *  - legacy format (kotlinx, no discriminator): {"row":"1","column":"A"} → PickedSeat
 */
class LegacySeatDeserializer : StdDeserializer<Seat>(Seat::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Seat {
        val node: JsonNode = p.readValueAsTree()
        val type = node.get("type")?.stringValue()
        return when {
            type == "AnySeat" -> AnySeat
            type == "PickedSeat" || (type == null && node.has("row")) ->
                PickedSeat(row = node.get("row").stringValue(), column = node.get("column").stringValue())
            else -> AnySeat
        }
    }
}

class TravelOfferSerializer : StdSerializer<TravelOffer>(TravelOffer::class.java) {
    override fun serialize(value: TravelOffer, gen: JsonGenerator, ctxt: SerializationContext) {
        gen.writeStartObject()
        gen.writeStringProperty("commuteId", value.commuteId.value.toString())
        gen.writeStringProperty("accommodationId", value.accommodationId.value.toString())
        if (value.attractionId is AttractionId.Present) {
            gen.writeStringProperty("attractionId", value.attractionId.value.toString())
        } else {
            gen.writeNullProperty("attractionId")
        }
        gen.writeEndObject()
    }
}

class TravelOfferDeserializer : StdDeserializer<TravelOffer>(TravelOffer::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): TravelOffer {
        val node: JsonNode = p.readValueAsTree()
        val commuteId = CommuteId.from(UUID.fromString(node.get("commuteId").stringValue()))
        val accommodationId = AccommodationId.from(UUID.fromString(node.get("accommodationId").stringValue()))
        val attractionIdNode = node.get("attractionId")
        val attractionId = if (attractionIdNode == null || attractionIdNode.isNull) {
            AttractionId.Empty
        } else {
            AttractionId.from(UUID.fromString(attractionIdNode.stringValue()))
        }
        return TravelOffer(commuteId, accommodationId, attractionId)
    }
}

class CommuteIdSerializer : StdSerializer<CommuteId>(CommuteId::class.java) {
    override fun serialize(value: CommuteId, gen: JsonGenerator, ctxt: SerializationContext) {
        gen.writeString(value.value.toString())
    }
}

class CommuteIdDeserializer : StdDeserializer<CommuteId>(CommuteId::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): CommuteId {
        return CommuteId.from(UUID.fromString(p.getValueAsString()))
    }
}

class AccommodationIdSerializer : StdSerializer<AccommodationId>(AccommodationId::class.java) {
    override fun serialize(value: AccommodationId, gen: JsonGenerator, ctxt: SerializationContext) {
        gen.writeString(value.value.toString())
    }
}

class AccommodationIdDeserializer : StdDeserializer<AccommodationId>(AccommodationId::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): AccommodationId {
        return AccommodationId.from(UUID.fromString(p.getValueAsString()))
    }
}

class AttractionIdSerializer : StdSerializer<AttractionId>(AttractionId::class.java) {
    override fun serialize(value: AttractionId, gen: JsonGenerator, ctxt: SerializationContext) {
        val v = value.value
        if (v == null) gen.writeNull() else gen.writeString(v.toString())
    }
}

class AttractionIdDeserializer : StdDeserializer<AttractionId>(AttractionId::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): AttractionId {
        return if (p.currentToken() == JsonToken.VALUE_NULL) {
            AttractionId.Empty
        } else {
            AttractionId.from(UUID.fromString(p.getValueAsString()))
        }
    }
}

class BookingIdSerializer : StdSerializer<BookingId>(BookingId::class.java) {
    override fun serialize(value: BookingId, gen: JsonGenerator, ctxt: SerializationContext) {
        val v = value.value
        if (v == null) gen.writeNull() else gen.writeString(v.toString())
    }
}

class BookingIdDeserializer : StdDeserializer<BookingId>(BookingId::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): BookingId {
        return if (p.currentToken() == JsonToken.VALUE_NULL) {
            BookingId.Empty
        } else {
            BookingId.from(UUID.fromString(p.getValueAsString()))
        }
    }
}

@Configuration
class JacksonConfig {

    @Bean
    @Primary
    fun objectMapper(): JsonMapper {
        val legacyModule = SimpleModule()
            .addDeserializer(UUID::class.java, LegacyUUIDDeserializer())
            .addDeserializer(Seat::class.java, LegacySeatDeserializer())
            .addSerializer(CommuteId::class.java, CommuteIdSerializer())
            .addDeserializer(CommuteId::class.java, CommuteIdDeserializer())
            .addSerializer(AccommodationId::class.java, AccommodationIdSerializer())
            .addDeserializer(AccommodationId::class.java, AccommodationIdDeserializer())
            .addSerializer(AttractionId::class.java, AttractionIdSerializer())
            .addDeserializer(AttractionId::class.java, AttractionIdDeserializer())
            .addSerializer(BookingId::class.java, BookingIdSerializer())
            .addDeserializer(BookingId::class.java, BookingIdDeserializer())
            .addSerializer(TravelOffer::class.java, TravelOfferSerializer())
            .addDeserializer(TravelOffer::class.java, TravelOfferDeserializer())

        return JsonMapper.builder()
            .addModule(legacyModule)
            .addModule(tools.jackson.module.kotlin.KotlinModule.Builder().build())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build()
    }
}
