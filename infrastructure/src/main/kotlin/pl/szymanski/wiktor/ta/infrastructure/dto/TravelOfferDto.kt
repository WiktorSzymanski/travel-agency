package pl.szymanski.wiktor.ta.infrastructure.dto

import kotlinx.serialization.Serializable
import pl.szymanski.wiktor.ta.domain.Booking
import pl.szymanski.wiktor.ta.domain.LocationAndTime
import pl.szymanski.wiktor.ta.domain.OfferStatusEnum
import pl.szymanski.wiktor.ta.domain.Rent
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer

@Serializable
data class TravelOfferDto(
    val id: String,
    val name: String,
    val commute: CommuteDto,
    val accommodation: AccommodationDto,
    val attraction: AttractionDto? = null,
    val booking: BookingDto? = null,
    val status: String = OfferStatusEnum.AVAILABLE.name,
) {
    companion object {
        fun fromDomain(
            travelOffer: TravelOffer,
            commute: Commute,
            accommodation: Accommodation,
            attraction: Attraction?,
        ): TravelOfferDto {
            return TravelOfferDto(
                id = travelOffer._id.toString(),
                name = travelOffer.name,
                commute = CommuteDto.fromDomain(commute),
                accommodation = AccommodationDto.fromDomain(accommodation),
                attraction = if (attraction != null) AttractionDto.fromDomain(attraction) else null,
                booking = travelOffer.booking?.let { BookingDto.fromDomain(it) },
                status = travelOffer.status.name
            )
        }
    }
}

@Serializable
data class BookingDto(
    val userId: String,
    val timestamp: String,
) {
    companion object {
        fun fromDomain(booking: Booking): BookingDto {
            return BookingDto(
                userId = booking.userId.toString(),
                timestamp = booking.timestamp.toString()
            )
        }
    }
}

@Serializable
data class CommuteDto(
    val id: String,
    val name: String,
    val departure: LocationAndTimeDto,
    val arrival: LocationAndTimeDto,
//    val seats: List<SeatDto>,
//    val bookings: Map<String, BookingDto> = mapOf(),
//    val status: String,
) {
    companion object {
        fun fromDomain(commute: Commute): CommuteDto {
            return CommuteDto(
                id = commute._id.toString(),
                name = commute.name,
                departure = LocationAndTimeDto.fromDomain(commute.departure),
                arrival = LocationAndTimeDto.fromDomain(commute.arrival),
//                seats = commute.seats.map { SeatDto.fromDomain(it) },
//                bookings = commute.bookings.mapValues { BookingDto.fromDomain(it.value) },
//                status = commute.status.name
            )
        }
    }
}

@Serializable
data class AttractionDto(
    val id: String,
    val name: String,
    val location: String,
    val date: String,
//    val capacity: Int,
//    val bookings: List<BookingDto> = listOf(),
//    val status: String,
) {
    companion object {
        fun fromDomain(attraction: Attraction): AttractionDto {
            return AttractionDto(
                id = attraction._id.toString(),
                name = attraction.name,
                location = attraction.location.name,
                date = attraction.date.toString(),
//                capacity = attraction.capacity,
//                bookings = attraction.bookings.map { BookingDto.fromDomain(it) },
//                status = attraction.status.name
            )
        }
    }
}

@Serializable
data class AccommodationDto(
    val id: String,
    val name: String,
    val location: String,
    val rent: RentDto,
    val booking: BookingDto? = null,
    val status: String,
) {
    companion object {
        fun fromDomain(accommodation: Accommodation): AccommodationDto {
            return AccommodationDto(
                id = accommodation._id.toString(),
                name = accommodation.name,
                location = accommodation.location.name,
                rent = RentDto.fromDomain(accommodation.rent),
                booking = accommodation.booking?.let { BookingDto.fromDomain(it) },
                status = accommodation.status.name
            )
        }
    }
}

@Serializable
data class LocationAndTimeDto(
    val location: String,
    val time: String,
) {
    companion object {
        fun fromDomain(locationAndTime: LocationAndTime): LocationAndTimeDto {
            return LocationAndTimeDto(
                location = locationAndTime.location.name,
                time = locationAndTime.time.toString()
            )
        }
    }
}

@Serializable
data class SeatDto(
    val row: String,
    val column: String,
) {
    companion object {
        fun fromDomain(seat: Seat): SeatDto {
            return SeatDto(
                row = seat.row,
                column = seat.column
            )
        }
    }
}

@Serializable
data class RentDto(
    val from: String,
    val till: String,
) {
    companion object {
        fun fromDomain(rent: Rent): RentDto {
            return RentDto(
                from = rent.from.toString(),
                till = rent.till.toString()
            )
        }
    }
}
