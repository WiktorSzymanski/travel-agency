package pl.szymanski.wiktor.ta.infrastructure.repository

import io.kurrent.dbclient.KurrentDBClient
import tools.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
import pl.szymanski.wiktor.ta.domain.event.AttractionEvent

@Component
@Primary
class KurrentAttractionRepository(
    client: KurrentDBClient,
    objectMapper: ObjectMapper
) : KurrentCommandRepository<Attraction, AttractionId, AttractionEvent>(
    client,
    objectMapper,
    Attraction::class,
    AttractionEvent::class,
    { events -> Attraction.fromEvents(events) }
)
