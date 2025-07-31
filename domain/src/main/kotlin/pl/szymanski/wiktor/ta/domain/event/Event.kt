package pl.szymanski.wiktor.ta.domain.event

import java.util.UUID

interface Event {
    val eventId: UUID
    var correlationId: UUID?
}

interface AccommodationEvent : Event {
    val accommodationId: UUID
}

interface AttractionEvent : Event {
    val attractionId: UUID
}

interface CommuteEvent : Event {
    val commuteId: UUID
}

interface TravelOfferEvent : Event {
    val travelOfferId: UUID
}
