package pl.szymanski.wiktor.ta.presentation.controller

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.TravelOfferStatusEnum
import pl.szymanski.wiktor.ta.dto.BookingDto
import pl.szymanski.wiktor.ta.query.BookingQuery
import pl.szymanski.wiktor.ta.query.TravelOfferQuery
import pl.szymanski.wiktor.ta.queryRepository.AccommodationQueryRepository
import pl.szymanski.wiktor.ta.queryRepository.TravelOfferQueryRepository
import java.util.*

fun Application.QueryController(
    travelOfferQueryRepository: TravelOfferQueryRepository,
    accommodationQueryRepository: AccommodationQueryRepository,
    bookingQuery: BookingQuery,
) {
    fun extractPaginationParams(queryParams: Parameters): Pair<Int, Int> {
        val page = queryParams["page"]?.toIntOrNull() ?: 1
        val size = queryParams["size"]?.toIntOrNull() ?: 20

        return Pair(page, size)
    }

    fun extractQueryParams(queryParams: Parameters): Triple<UUID, UUID, Seat?> {
        val offerId = queryParams["offerId"]?.let { UUID.fromString(it) }
        val userId = queryParams["userId"]?.let { UUID.fromString(it) }
        val seat = queryParams["seat"]?.let { Seat.fromString(it) }

        requireNotNull(offerId)
        requireNotNull(userId)

        return Triple(offerId, userId, seat)
    }

    val travelOfferQuery =
        TravelOfferQuery(
            travelOfferRepository = travelOfferQueryRepository,
            accommodationRepository = accommodationQueryRepository,
        )

    routing {
        get("/travelOffers") {
            val (page, size) = extractPaginationParams(call.request.queryParameters)
            val resp = travelOfferQuery.getTravelOffers(page, size)
            call.response.status(HttpStatusCode.OK)
            call.respond(resp)
        }

        get("/travelOffers/{status}/count") {
            val status = call.parameters["status"]?.let { TravelOfferStatusEnum.valueOf(it) }

            requireNotNull(status) {
                "Invalid travel offer status ${call.parameters["status"]}"
            }

            val count = travelOfferQuery.countTravelOffersByStatus(status)
            call.response.status(HttpStatusCode.OK)
            call.respond(mapOf("count" to count))
        }

        get("/booking/{bookingId}") {
            val bookingId = call.parameters["bookingId"]?.let { UUID.fromString(it) }

            requireNotNull(bookingId)
            bookingQuery.getBookingById(bookingId).let {
                call.response.status(HttpStatusCode.OK)
                call.respond(BookingDto.fromDomain(it))
            }
        }

        get("/userBookings/{userId}") {
            val (page, size) = extractPaginationParams(call.request.queryParameters)
            val userId = call.parameters["userId"]?.let { UUID.fromString(it) }

            requireNotNull(userId)
            bookingQuery.getBookingsByUserId(page, size, userId).let {
                call.response.status(HttpStatusCode.OK)
                call.respond(it.map { booking -> BookingDto.fromDomain(booking) })
            }
        }

        get("/travelOffersUser/{userId}") {
            val (page, size) = extractPaginationParams(call.request.queryParameters)
            val userId = call.parameters["userId"]?.let { UUID.fromString(it) }

            requireNotNull(userId)
            val resp = bookingQuery.getTravelOffersByUserId(page, size, userId)
            call.response.status(HttpStatusCode.OK)
            call.respond(resp)
        }

        get("/travelOffers/location/{location}") {
            val (page, size) = extractPaginationParams(call.request.queryParameters)
            val location = call.parameters["location"]?.let { LocationEnum.valueOf(it) }
            val status = call.request.queryParameters["status"]?.let { TravelOfferStatusEnum.valueOf(it) }
            requireNotNull(location)
            requireNotNull(status)
            val resp = travelOfferQuery.getTravelOfferByLocation(page, size, location, status)
            call.response.status(HttpStatusCode.OK)
            call.respond(resp)
        }

        get("/travelOffer/{id}") {
            val travelOfferId = call.parameters["id"]?.let { UUID.fromString(it) }

            requireNotNull(travelOfferId) {
                "Invalid travel offer Id: ${call.parameters["id"]}"
            }

            val resp = travelOfferQuery.getTravelOfferById(travelOfferId)
            call.response.status(HttpStatusCode.OK)
            call.respond(resp)
        }

        // OPTIONS to check how many travelOffers are available so when picking at random, not restricted to the first 20 or so
        get("/travelOffers/{status}") {
            val status = call.parameters["status"]?.let { TravelOfferStatusEnum.valueOf(it) }
            val (page, size) = extractPaginationParams(call.request.queryParameters)

            requireNotNull(status) {
                "Invalid travel offer status ${call.parameters["status"]}"
            }

            val resp =
                travelOfferQuery.getTravelOffersByStatus(
                    status,
                    page,
                    size,
                )
            call.response.status(HttpStatusCode.OK)
            call.respond(resp)
        }
    }

    routing {
        swaggerUI(path = "swaggerUI")
    }
}
