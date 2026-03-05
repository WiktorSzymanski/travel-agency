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
@RequestMapping("/api/travelOffer")
class TravelOfferController(
    private val commuteQuery: CommuteQuery,
    private val attractionQuery: AttractionQuery,
    private val accommodationQuery: AccommodationQuery
) {

    @GetMapping("/any")
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
}
