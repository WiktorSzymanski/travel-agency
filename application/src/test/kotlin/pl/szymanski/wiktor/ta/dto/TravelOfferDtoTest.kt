package pl.szymanski.wiktor.ta.dto

import kotlin.test.Test
import kotlin.test.assertEquals
import pl.szymanski.wiktor.ta.domain.*
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOfferId
import java.time.LocalDateTime
import java.util.UUID

class TravelOfferDtoTest {
    private fun sampleCommute(): Commute {
        val dep = LocationAndTime(LocationEnum.POZNAN, LocalDateTime.of(2025, 3, 1, 9, 0))
        val arr = LocationAndTime(LocationEnum.BERLIN, LocalDateTime.of(2025, 3, 1, 12, 0))
        return Commute(
            name = "Commute A",
            departure = dep,
            arrival = arr,
            seats = listOf(Seat.Picked("A", "1"), Seat.Picked("A", "2"))
        )
    }

    private fun sampleAccommodation(): Accommodation {
        return Accommodation(
            name = "Stay Inn",
            location = LocationEnum.VIENNA,
            rent = Rent(
                from = LocalDateTime.of(2025, 3, 1, 14, 0),
                till = LocalDateTime.of(2025, 3, 5, 10, 0)
            )
        )
    }

    private fun sampleAttraction(): Attraction {
        return Attraction(
            name = "City Walk",
            location = LocationEnum.VIENNA,
            date = LocalDateTime.of(2025, 3, 2, 16, 0),
            capacity = 10,
        )
    }

    @Test
    fun `fromDomain maps nested dtos and fields with attraction`() {
        val commute = sampleCommute()
        val accommodation = sampleAccommodation()
        val attraction = sampleAttraction()
        val bookingId = BookingId.generate()

        val travelOffer = TravelOffer(
            id = TravelOfferId.generate(commute.id, accommodation.id, attraction.id),
            name = "Offer 1",
            commuteId = commute.id,
            accommodationId = accommodation.id,
            attractionId = attraction.id,
            bookingId = bookingId,
            status = TravelOfferStatusEnum.BOOKED,
        )

        val dto = TravelOfferDto.fromDomain(travelOffer, commute, accommodation, attraction)

        assertEquals(travelOffer.id.toString(), dto.id)
        assertEquals("Offer 1", dto.name)
        assertEquals(commute.id.toString(), dto.commute.id)
        assertEquals(accommodation.id.toString(), dto.accommodation.id)
        assert(dto.attraction is AttractionDto.Present)
        assertEquals(attraction.id.toString(), (dto.attraction as AttractionDto.Present).id)
        assertEquals(bookingId.toString(), dto.booking)
        assertEquals("BOOKED", dto.status)
    }

    @Test
    fun `fromDomain maps fields without attraction`() {
        val commute = sampleCommute()
        val accommodation = sampleAccommodation()

        val travelOffer = TravelOffer(
            id = TravelOfferId.generate(commute.id, accommodation.id, AttractionId.Empty),
            name = "Offer 2",
            commuteId = commute.id,
            accommodationId = accommodation.id,
            attractionId = AttractionId.Empty,
            bookingId = BookingId.Empty,
            status = TravelOfferStatusEnum.AVAILABLE,
        )

        val dto = TravelOfferDto.fromDomain(travelOffer, commute, accommodation, null)

        assertEquals(travelOffer.id.toString(), dto.id)
        assertEquals("Offer 2", dto.name)
        assertEquals(commute.id.toString(), dto.commute.id)
        assertEquals(accommodation.id.toString(), dto.accommodation.id)
        assertEquals(dto.attraction.toString(), AttractionId.Empty.toString())
        assertEquals(dto.booking, BookingId.Empty.toString())
        assertEquals("AVAILABLE", dto.status)
    }
}
