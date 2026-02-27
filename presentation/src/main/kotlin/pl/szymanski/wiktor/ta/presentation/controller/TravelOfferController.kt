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
import pl.szymanski.wiktor.ta.infrastructure.dto.AccommodationDto
import pl.szymanski.wiktor.ta.infrastructure.dto.AttractionDto
import pl.szymanski.wiktor.ta.infrastructure.dto.CommuteDto
import pl.szymanski.wiktor.ta.infrastructure.dto.CreateBookingRequest
import pl.szymanski.wiktor.ta.infrastructure.dto.TravelOfferDto
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

    /**
     * Get all travel offers with pagination
     * @param page Page number (default: 1)
     * @param size Page size (default: 20)
     */
    @GetMapping("/scheduledCommutes")
    fun getScheduledCommutes(
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<List<CommuteDto>> = runBlocking {
        //TODO: pagination
        val resp = commuteQuery.getScheduledCommutes().map { CommuteDto.fromDomain(it) }
        ResponseEntity.ok(resp)
    }

    @GetMapping("/scheduledAttractions")
    fun getScheduledAttractions(
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<List<AttractionDto>> = runBlocking {
        //TODO: pagination
        val resp = attractionQuery.getScheduledAttractions().map { AttractionDto.fromDomain(it)}
        ResponseEntity.ok(resp)
    }

    @GetMapping("/availableAccommodations")
    fun getAvailableAccommodations(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<Page<AccommodationDto>> = runBlocking {
        val resp = accommodationQuery.getAvailableAccommodations(Pageable(page, size))
            .map { AccommodationDto.fromDomain(it) }
        ResponseEntity.ok(resp)
    }

    @GetMapping("/anyTravelOffer")
    fun getAnyTravelOffer() = runBlocking {
        val commute = commuteQuery.getScheduledCommutes()[0]
        val accommodation = accommodationQuery.getAvailableAccommodations(Pageable(0, 1)).content[0]
        val attraction = attractionQuery.getScheduledAttractions()[0]

        ResponseEntity.ok(TravelOfferDto.fromDomain(
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


