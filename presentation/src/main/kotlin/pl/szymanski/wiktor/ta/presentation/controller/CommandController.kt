package pl.szymanski.wiktor.ta.presentation.controller

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import pl.szymanski.wiktor.ta.command.BookingCommand
import pl.szymanski.wiktor.ta.command.BookingRequestCancelCommand
import pl.szymanski.wiktor.ta.command.CreateBookingCommand
import pl.szymanski.wiktor.ta.commandHandler.BookingCommandHandler
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.event.BookingCancelRequestedFailedEvent
import java.util.*

fun Application.CommandController(
    bookingCommandHandler: BookingCommandHandler,
) {
    fun extractQueryParams(queryParams: Parameters): Triple<UUID, UUID, Seat?> {
        val offerId = queryParams["offerId"]?.let { UUID.fromString(it) }
        val userId = queryParams["userId"]?.let { UUID.fromString(it) }
        val seat = queryParams["seat"]?.let { Seat.fromString(it) }

        requireNotNull(offerId)
        requireNotNull(userId)

        return Triple(offerId, userId, seat)
    }

    routing {
        post("/bookTravelOffer") {
            val (offerId, userId, seat) = extractQueryParams(call.request.queryParameters)

            val bookingId = bookingCommandHandler.handle(
                CreateBookingCommand(
                    correlationId = UUID.randomUUID(),
                    userId = userId,
                    travelOfferId = offerId,
                    seat = seat,
                ) as BookingCommand,
            ).bookingId

            call.respond(HttpStatusCode.Accepted, mapOf("bookingId" to bookingId.toString()))
        }
        post("/cancelTravelOffer") {
            val bookingId = call.request.queryParameters["bookingId"]?.let { UUID.fromString(it) }

            requireNotNull(bookingId)

            val event = bookingCommandHandler.handle(
                BookingRequestCancelCommand(
                    correlationId = UUID.randomUUID(),
                    bookingId = bookingId,
                )
            )

            if (event is BookingCancelRequestedFailedEvent) {
                call.respond(HttpStatusCode.Forbidden, event.message)
            } else {
                call.respond(HttpStatusCode.Accepted, mapOf("bookingId" to bookingId.toString()))
            }
        }
    }

    routing {
        swaggerUI(path = "swaggerUI")
    }
}
