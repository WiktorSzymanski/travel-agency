package pl.szymanski.wiktor.ta.saga

import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.event.SagaEvent
import java.util.UUID

interface SagaOutboxPort {
    suspend fun saveStateWithEvent(
        sagaState: SagaState,
        event: EventEnvelope<SagaEvent>
    ): UUID

    suspend fun getPendingEvents(limit: Int = 100): List<SagaOutboxEntry>
    suspend fun markAsPublished(eventId: UUID)
}