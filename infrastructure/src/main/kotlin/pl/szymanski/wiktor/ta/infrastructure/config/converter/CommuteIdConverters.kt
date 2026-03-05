package pl.szymanski.wiktor.ta.infrastructure.config.converter

import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import pl.szymanski.wiktor.ta.domain.aggregate.CommuteId
import java.util.UUID

@WritingConverter
class CommuteIdToUUIDConverter : Converter<CommuteId, UUID> {
    override fun convert(source: CommuteId): UUID = source.value
}

@ReadingConverter
class UUIDToCommuteIdConverter : Converter<UUID, CommuteId> {
    override fun convert(source: UUID): CommuteId = CommuteId.from(source)
}
