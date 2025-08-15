package pl.szymanski.wiktor.ta.domain.repository

import pl.szymanski.wiktor.ta.domain.event.Event

interface EventRepository {
    suspend fun save(event: Event, revision: Int)
    suspend fun noRevisionSave(event: Event)
    suspend fun subscribe(eventClass: Class<Event>, onEvent: suspend (Event) -> Unit)
}