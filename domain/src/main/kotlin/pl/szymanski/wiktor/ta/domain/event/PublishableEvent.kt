package pl.szymanski.wiktor.ta.domain.event

import java.util.UUID

interface PublishableEvent {
    val eventId: UUID
}