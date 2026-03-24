package pl.szymanski.wiktor.ta.infrastructure.repository

import io.kurrent.dbclient.KurrentDBClient
import tools.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.AccommodationId
import pl.szymanski.wiktor.ta.domain.event.AccommodationEvent

@Component
@Primary
class KurrentAccommodationRepository(
    client: KurrentDBClient,
    objectMapper: ObjectMapper
) : KurrentCommandRepository<Accommodation, AccommodationId, AccommodationEvent>(
    client,
    objectMapper,
    Accommodation::class,
    AccommodationEvent::class,
    { events -> Accommodation.fromEvents(events) }
)
