package pl.szymanski.wiktor.ta.infrastructure.config.converter

import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
import java.util.UUID

@WritingConverter
class AttractionIdToUUIDConverter : Converter<AttractionId, UUID> {
    override fun convert(source: AttractionId): UUID {
        return source.value
            ?: throw IllegalArgumentException("Cannot convert AttractionId.Empty to UUID for MongoDB _id")
    }
}

@ReadingConverter
class UUIDToAttractionIdConverter : Converter<UUID, AttractionId> {
    override fun convert(source: UUID): AttractionId = AttractionId.from(source)
}
