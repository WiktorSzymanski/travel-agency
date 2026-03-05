package pl.szymanski.wiktor.ta.presentation.controller

import kotlinx.coroutines.runBlocking
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import pl.szymanski.wiktor.ta.Page
import pl.szymanski.wiktor.ta.Pageable
import pl.szymanski.wiktor.ta.infrastructure.document.AccommodationDocument
import pl.szymanski.wiktor.ta.query.AccommodationQuery

@RestController
@RequestMapping("/api/accommodation")
class AccommodationController(
    private val accommodationQuery: AccommodationQuery
) {

    @GetMapping
    fun getAvailableAccommodations(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<Page<AccommodationDocument>> = runBlocking {
        val resp = accommodationQuery.getAvailableAccommodations(Pageable(page, size))
            .map { AccommodationDocument.fromDomain(it) }
        ResponseEntity.ok(resp)
    }
}
