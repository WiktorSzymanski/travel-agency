package pl.szymanski.wiktor.ta.saga

import pl.szymanski.wiktor.ta.domain.aggregate.AggregateId
import java.util.UUID

data class DeadLetterQueueEntry (
    val sagaId: UUID,
    val aggregateId: AggregateId,
    val message: String
)