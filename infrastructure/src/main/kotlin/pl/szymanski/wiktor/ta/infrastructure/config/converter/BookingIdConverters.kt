package pl.szymanski.wiktor.ta.infrastructure.config.converter

import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import java.util.UUID

@WritingConverter
class BookingIdToUUIDConverter : Converter<BookingId, UUID> {
    override fun convert(source: BookingId): UUID {
        return source.value
            ?: throw IllegalArgumentException("Cannot convert BookingId.Empty to UUID for MongoDB _id")
    }
}

@ReadingConverter
class UUIDToBookingIdConverter : Converter<UUID, BookingId> {
    override fun convert(source: UUID): BookingId = BookingId.from(source)
}
