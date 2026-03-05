package pl.szymanski.wiktor.ta.presentation.controller

import kotlinx.coroutines.runBlocking
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import pl.szymanski.wiktor.ta.infrastructure.document.BookingDocument
import pl.szymanski.wiktor.ta.infrastructure.document.CreateBookingRequest
import pl.szymanski.wiktor.ta.query.BookingQuery
import pl.szymanski.wiktor.ta.service.BookingService
import java.util.UUID

@RestController
@RequestMapping("/api/bookings")
class BookingController(
    private val bookingQuery: BookingQuery,
    private val bookingService: BookingService,
) {

    @GetMapping("/{id}")
    fun getBookingById(@PathVariable id: UUID): ResponseEntity<BookingDocument> = runBlocking {
        val bookingId = BookingId.from(id) as BookingId.Present
        val (booking, version) = bookingQuery.getBookingById(bookingId)
        ResponseEntity.ok(BookingDocument.fromDomain(booking, version))
    }

    @PostMapping("/book")
    fun createBooking(@RequestBody request: CreateBookingRequest): ResponseEntity<Map<String, String>> = runBlocking {
        val bookingId = bookingService.createBooking(
            userId = request.userIdAsUUID(),
            travelOffer = request.travelOfferToDomain(),
            seat = request.seat.toDomain(),
        )
        ResponseEntity.status(HttpStatus.CREATED)
            .body(mapOf("bookingId" to bookingId.value.toString()))
    }

    @PostMapping("/{id}/cancel")
    fun cancelBooking(@PathVariable id: UUID): ResponseEntity<Map<String, String>> = runBlocking {
        bookingService.requestCancelBooking(BookingId.from(id) as BookingId.Present)
        ResponseEntity.ok(mapOf("bookingId" to id.toString()))
    }
}
