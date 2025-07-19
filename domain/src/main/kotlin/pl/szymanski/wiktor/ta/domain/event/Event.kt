package pl.szymanski.wiktor.ta.domain.event

import java.util.UUID

interface Event {
    val eventId: UUID
    var correlationId: UUID?
}
