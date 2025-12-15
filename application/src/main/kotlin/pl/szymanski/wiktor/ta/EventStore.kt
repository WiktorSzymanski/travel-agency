package pl.szymanski.wiktor.ta

import pl.szymanski.wiktor.ta.domain.event.DomainEvent

interface EventStore {
    suspend fun append(event: DomainEvent, revision: Int)
    suspend fun appendNoRevision(event: DomainEvent)
    suspend fun getEvents(aggregateId: String): List<DomainEvent>
}