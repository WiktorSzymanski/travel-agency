package pl.szymanski.wiktor.ta.infrastructure.config.converter

import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import pl.szymanski.wiktor.ta.domain.aggregate.AccommodationId
import java.util.UUID

@WritingConverter
class AccommodationIdToUUIDConverter : Converter<AccommodationId, UUID> {
    override fun convert(source: AccommodationId): UUID = source.value
}

@ReadingConverter
class UUIDToAccommodationIdConverter : Converter<UUID, AccommodationId> {
    override fun convert(source: UUID): AccommodationId = AccommodationId.from(source)
}
