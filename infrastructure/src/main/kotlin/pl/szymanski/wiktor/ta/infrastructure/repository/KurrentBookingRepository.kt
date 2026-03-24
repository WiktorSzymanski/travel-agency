package pl.szymanski.wiktor.ta.infrastructure.repository

import io.kurrent.dbclient.KurrentDBClient
import tools.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import pl.szymanski.wiktor.ta.domain.aggregate.Booking
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import pl.szymanski.wiktor.ta.domain.event.BookingEvent

@Component
@Primary
class KurrentBookingRepository(
    client: KurrentDBClient,
    objectMapper: ObjectMapper
) : KurrentCommandRepository<Booking, BookingId, BookingEvent>(
    client,
    objectMapper,
    Booking::class,
    BookingEvent::class,
    { events -> Booking.fromEvents(events) }
)
