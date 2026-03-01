package pl.szymanski.wiktor.ta.presentation.controller

import kotlinx.coroutines.runBlocking
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import pl.szymanski.wiktor.ta.Page
import pl.szymanski.wiktor.ta.Pageable
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import pl.szymanski.wiktor.ta.infrastructure.document.AccommodationDocument
import pl.szymanski.wiktor.ta.infrastructure.document.AttractionDocument
import pl.szymanski.wiktor.ta.infrastructure.document.CommuteDocument
import pl.szymanski.wiktor.ta.infrastructure.document.CreateBookingRequest
import pl.szymanski.wiktor.ta.infrastructure.document.TravelOfferDocument
import pl.szymanski.wiktor.ta.query.AccommodationQuery
import pl.szymanski.wiktor.ta.query.AttractionQuery
import pl.szymanski.wiktor.ta.query.CommuteQuery
import pl.szymanski.wiktor.ta.service.BookingService

@RestController
@RequestMapping("/api")
class CommuteController(
    private val commuteQuery: CommuteQuery,
    private val bookingService: BookingService,
    private val attractionQuery: AttractionQuery,
    private val accommodationQuery: AccommodationQuery
) {

    @GetMapping("/scheduledCommutes")
    fun getScheduledCommutes(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<Page<CommuteDocument>> = runBlocking {
        val resp = commuteQuery.getScheduledCommutes(Pageable(page, size)).map { CommuteDocument.fromDomain(it) }
        ResponseEntity.ok(resp)
    }

    @GetMapping("/scheduledAttractions")
    fun getScheduledAttractions(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<Page<AttractionDocument>> = runBlocking {
        val resp = attractionQuery.getScheduledAttractions(Pageable(page, size)).map { AttractionDocument.fromDomain(it)}
        ResponseEntity.ok(resp)
    }

    @GetMapping("/availableAccommodations")
    fun getAvailableAccommodations(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<Page<AccommodationDocument>> = runBlocking {
        val resp = accommodationQuery.getAvailableAccommodations(Pageable(page, size))
            .map { AccommodationDocument.fromDomain(it) }
        ResponseEntity.ok(resp)
    }

    @GetMapping("/anyTravelOffer")
    fun getAnyTravelOffer() = runBlocking {
        val commute = commuteQuery.getScheduledCommutes(Pageable(0, 1)).content[0]
        val accommodation = accommodationQuery.getAvailableAccommodations(Pageable(0, 1)).content[0]
        val attraction = attractionQuery.getScheduledAttractions(Pageable(0, 1)).content[0]

        ResponseEntity.ok(TravelOfferDocument.fromDomain(
            TravelOffer(
                commute.id,
                accommodation.id,
                attraction.id
            )
        ))
    }


    @PostMapping("/bookings")
    fun createBooking(@RequestBody request: CreateBookingRequest): ResponseEntity<Map<String, String>> = runBlocking {
        val bookingId = bookingService.createBooking(
            userId = request.userIdAsUUID(),
            travelOffer = request.travelOfferToDomain(),
            seat = request.seat.toDomain(),
        )
        ResponseEntity.status(HttpStatus.CREATED)
            .body(mapOf("message" to "Booking ${bookingId.value.toString()} created successfully"))
    }
}
