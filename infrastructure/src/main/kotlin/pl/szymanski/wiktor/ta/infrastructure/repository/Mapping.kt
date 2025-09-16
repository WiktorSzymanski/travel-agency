package pl.szymanski.wiktor.ta.infrastructure.repository

import org.bson.Document
import pl.szymanski.wiktor.ta.domain.LocationAndTime
import pl.szymanski.wiktor.ta.domain.Rent
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.TravelOfferStatusEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import pl.szymanski.wiktor.ta.dto.AccommodationDto
import pl.szymanski.wiktor.ta.dto.AttractionDto
//import pl.szymanski.wiktor.ta.dto.BookingDto
import pl.szymanski.wiktor.ta.dto.CommuteDto
import pl.szymanski.wiktor.ta.dto.LocationAndTimeDto
import pl.szymanski.wiktor.ta.dto.RentDto
import pl.szymanski.wiktor.ta.dto.TravelOfferDto
import java.time.LocalDateTime
import java.util.UUID
import kotlin.collections.emptyMap

fun Document.toRentDto(): RentDto =
    RentDto(
        from = get("from", LocalDateTime::class).toString(),
        till = get("till", LocalDateTime::class).toString(),
    )

fun Document.toLocationAndTimeDto(): LocationAndTimeDto =
    LocationAndTimeDto(
        location = getString("location"),
        time = get("time", LocalDateTime::class).toString(),
    )

//fun Document.toBookingDto(): BookingDto =
//    BookingDto(
//        userId = get("userId", UUID::class).toString(),
//        timestamp = get("timestamp", LocalDateTime::class).toString(),
//    )

fun Accommodation.toDto(): AccommodationDto =
    AccommodationDto(
        id = id.toString(),
        name = name,
        location = location.toString(),
        rent = rent.toDto(),
        booking = bookingId?.toString(),
        status = status.toString()
    )

fun Rent.toDto(): RentDto =
    RentDto(
        from = from.toString(),
        till = till.toString()
    )

fun Attraction.toDto(): AttractionDto =
    AttractionDto(
        id = id.toString(),
        name = name,
        location = location.toString(),
        date = date.toString(),
        availableSlots = capacity - bookings.size
    )

fun Commute.toDto(): CommuteDto =
    CommuteDto(
        id = id.toString(),
        name = name,
        departure = departure.toDto(),
        arrival = arrival.toDto(),
        availableSeats = seats
            .map { "${it.row}${it.column}" }
            .filter { seatKey -> !bookings.containsKey(seatKey) }
    )

fun LocationAndTime.toDto(): LocationAndTimeDto =
    LocationAndTimeDto(
        location = location.name,
        time = time.toString()
    )


fun TravelOffer.toTravelOfferDto(
    accommodation: AccommodationDto?,
    attraction: AttractionDto?,
    commute: CommuteDto?
): TravelOfferDto = TravelOfferDto(
    id = this.id.toString(),
    name = this.name,
    commute = commute ?: error("Commute missing for offer $id"),
    accommodation = accommodation ?: error("Accommodation missing for offer $id"),
    attraction = attraction, // can be null
    booking = this.bookingId?.toString(),
    status = this.status.toString(),
)
