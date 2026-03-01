package pl.szymanski.wiktor.ta.presentation.controller

import kotlinx.coroutines.runBlocking
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import pl.szymanski.wiktor.ta.infrastructure.document.BookingDocument
import pl.szymanski.wiktor.ta.query.BookingQuery
import java.util.UUID

@RestController
@RequestMapping("/api/bookings")
class BookingController(
    private val bookingQuery: BookingQuery,
) {

    @GetMapping("/{id}")
    fun getBookingById(@PathVariable id: UUID): ResponseEntity<BookingDocument> = runBlocking {
        val bookingId = BookingId.from(id) as BookingId.Present
        val (booking, version) = bookingQuery.getBookingById(bookingId)
        ResponseEntity.ok(BookingDocument.fromDomain(booking, version))
    }
}

