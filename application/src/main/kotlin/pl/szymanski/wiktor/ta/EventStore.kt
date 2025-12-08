package pl.szymanski.wiktor.ta

import pl.szymanski.wiktor.ta.domain.event.Event

interface EventStore {
    suspend fun append(event: Event, revision: Int)
    suspend fun appendNoRevision(event: Event)
    suspend fun getEvents(aggregateId: String): List<Event>
}