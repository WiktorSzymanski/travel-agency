package pl.szymanski.wiktor.ta.offermaker

import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.domain.LocationAndTime
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.Rent
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.aggregate.AccommodationId
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
import pl.szymanski.wiktor.ta.domain.aggregate.CommuteId
import pl.szymanski.wiktor.ta.domain.event.AccommodationCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteCreatedEvent
import java.time.LocalDateTime
import java.util.UUID

fun getAccommodationCreatedEvent(location: LocationEnum, secondsInFuture: Long) = EventEnvelope(
    AccommodationCreatedEvent(
        UUID.randomUUID(),
        AccommodationId.generate(),
        "accommodation",
        location,
        Rent(
            LocalDateTime.now().plusSeconds(secondsInFuture),
            LocalDateTime.now().plusSeconds(secondsInFuture + 1),
        )
    ),
    Metadata(
        UUID.randomUUID(),
        0
    )
)

fun getCommuteCreatedEvent(location: LocationEnum, secondsInFuture: Long) =  EventEnvelope(
    CommuteCreatedEvent(
        UUID.randomUUID(),
        CommuteId.generate(),
        "commute",
        LocationAndTime(LocationEnum.POZNAN, LocalDateTime.now().plusSeconds(secondsInFuture)),
        LocationAndTime(location, LocalDateTime.now().plusSeconds(secondsInFuture + 1)),
        listOf(Seat.Any)
    ),
    Metadata(
        UUID.randomUUID(),
        0
    )
)

fun getAttractionCreatedEvent(location: LocationEnum, secondsInFuture: Long) =  EventEnvelope(
    AttractionCreatedEvent(
        UUID.randomUUID(),
        AttractionId.generate(),
        "attraction",
        location,
        LocalDateTime.now().plusSeconds(secondsInFuture),
        5
    ),
    Metadata(
        UUID.randomUUID(),
        0
    )
)