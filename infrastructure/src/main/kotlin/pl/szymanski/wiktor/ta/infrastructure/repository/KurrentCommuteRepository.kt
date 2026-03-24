package pl.szymanski.wiktor.ta.infrastructure.repository

import io.kurrent.dbclient.KurrentDBClient
import tools.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.aggregate.CommuteId
import pl.szymanski.wiktor.ta.domain.event.CommuteEvent

@Component
@Primary
class KurrentCommuteRepository(
    client: KurrentDBClient,
    objectMapper: ObjectMapper
) : KurrentCommandRepository<Commute, CommuteId, CommuteEvent>(
    client,
    objectMapper,
    Commute::class,
    CommuteEvent::class,
    { events -> Commute.fromEvents(events) }
)
