package pl.szymanski.wiktor.ta.infrastructure.repository

import org.bson.Document
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.TravelOfferStatusEnum
import pl.szymanski.wiktor.ta.dto.AccommodationDto
import pl.szymanski.wiktor.ta.dto.AttractionDto
import pl.szymanski.wiktor.ta.dto.BookingDto
import pl.szymanski.wiktor.ta.dto.CommuteDto
import pl.szymanski.wiktor.ta.dto.LocationAndTimeDto
import pl.szymanski.wiktor.ta.dto.RentDto
import pl.szymanski.wiktor.ta.dto.TravelOfferDto
import java.time.LocalDateTime
import java.util.UUID
import kotlin.collections.emptyMap


fun Document.toRentDto(): RentDto = RentDto(
    from = get("from", LocalDateTime::class).toString(),
    till = get("till", LocalDateTime::class).toString()
)

fun Document.toLocationAndTimeDto(): LocationAndTimeDto = LocationAndTimeDto(
    location = getString("location"),
    time = get("time", LocalDateTime::class).toString()
)

fun Document.toBookingDto(): BookingDto = BookingDto(
    userId = get("userId", UUID::class).toString(),
    timestamp = get("timestamp", LocalDateTime::class).toString()
)

fun Document.toCommuteDto(): CommuteDto = CommuteDto(
    id = get("_id").toString(),
    name = getString("name"),
    departure = (get("departure") as Document).toLocationAndTimeDto(),
    arrival = (get("arrival") as Document).toLocationAndTimeDto(),
    availableSeats = getList("seats", Document::class.java)
        .map { Seat(it.getString("row"), it.getString("column")).toString() }
        .filter { !(get("bookings") as? Map<*, *> ?: emptyMap<Any, Any>()).contains(it) }
)

fun Document.toAccommodationDto(): AccommodationDto = AccommodationDto(
    id = get("_id").toString(),
    name = getString("name"),
    location = getString("location"),
    rent = (get("rent") as Document).toRentDto(),
    booking = (get("booking") as? Document)?.toBookingDto(),
    status = getString("status")
)

fun Document.toAttractionDto(): AttractionDto = AttractionDto(
    id = get("_id").toString(),
    name = getString("name"),
    location = getString("location"),
    date = get("date", LocalDateTime::class).toString(),
    availableSlots = getInteger("capacity") - getList("bookings", Document::class.java).size,
)

fun Document.toTravelOfferDto(): TravelOfferDto = TravelOfferDto(
    id = get("_id").toString(),
    name = getString("name"),
    commute = (get("commute") as Document).toCommuteDto(),
    accommodation = (get("accommodation") as Document).toAccommodationDto(),
    attraction = (get("attraction") as? Document)?.toAttractionDto(),
    booking = (get("booking") as? Document)?.toBookingDto(),
    status = getString("status") ?: TravelOfferStatusEnum.AVAILABLE.name
)