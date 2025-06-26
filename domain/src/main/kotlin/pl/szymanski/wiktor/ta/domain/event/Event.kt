package pl.szymanski.wiktor.ta.domain.event

import java.util.UUID

sealed interface DomainEvent {
    val eventId: UUID
}