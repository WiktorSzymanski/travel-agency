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
import pl.szymanski.wiktor.ta.infrastructure.dto.CommuteDto
import pl.szymanski.wiktor.ta.infrastructure.dto.CreateBookingRequest
import pl.szymanski.wiktor.ta.query.CommuteQuery
import pl.szymanski.wiktor.ta.service.BookingService

@RestController
@RequestMapping("/api")
class CommuteController(
    private val commuteQuery: CommuteQuery,
    private val bookingService: BookingService,
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

    @PostMapping("/bookings")
    fun createBooking(@RequestBody request: CreateBookingRequest): ResponseEntity<Map<String, String>> = runBlocking {
        bookingService.createBooking(
            userId = request.userIdAsUUID(),
            travelOffer = request.travelOfferToDomain(),
            seat = request.seat.toDomain(),
        )
        ResponseEntity.status(HttpStatus.CREATED)
            .body(mapOf("message" to "Booking created successfully"))
    }
}


