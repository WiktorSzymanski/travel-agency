package pl.szymanski.wiktor.ta.infrastructure.function

import com.azure.cosmos.models.CosmosQueryRequestOptions
import com.azure.cosmos.models.SqlParameter
import com.azure.cosmos.models.SqlQuerySpec
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.BindingName
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import pl.szymanski.wiktor.ta.command.BookingCommand
import pl.szymanski.wiktor.ta.command.BookingRequestCancelCommand
import pl.szymanski.wiktor.ta.command.CreateBookingCommand
import pl.szymanski.wiktor.ta.commandHandler.BookingCommandHandler
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.TravelOfferStatusEnum
import pl.szymanski.wiktor.ta.domain.event.BookingCancelRequestedFailedEvent
import pl.szymanski.wiktor.ta.dto.BookingDto
import pl.szymanski.wiktor.ta.infrastructure.repository.command.BookingRepositoryImpl
import pl.szymanski.wiktor.ta.infrastructure.repository.query.BookingQueryRepositoryImpl
import pl.szymanski.wiktor.ta.infrastructure.scheduler.CosmosClientProjectionProvider
import pl.szymanski.wiktor.ta.infrastructure.scheduler.CosmosClientProvider
import pl.szymanski.wiktor.ta.query.BookingQuery
import java.util.Optional
import java.util.UUID
import kotlin.toString

val bookingCommandHandler = BookingCommandHandler(
    bookingRepository = BookingRepositoryImpl()
)

val bookingQuery = BookingQuery(BookingQueryRepositoryImpl())

fun extractPaginationParams(queryParameters: Map<String, String>?): Pair<Int, Int> {
    val page = queryParameters?.get("page")?.toIntOrNull() ?: 0
    val size = queryParameters?.get("size")?.toIntOrNull() ?: 20
    return Pair(page, size)
}

fun extractQueryParams(queryParams: Map<String, String>): Triple<UUID, UUID, Seat?> {
    val offerId = queryParams["offerId"]?.let { UUID.fromString(it) }
    val userId = queryParams["userId"]?.let { UUID.fromString(it) }
    val seat = queryParams["seat"]?.let { Seat.fromString(it) }

    requireNotNull(offerId)
    requireNotNull(userId)

    return Triple(offerId, userId, seat)
}

@FunctionName("GetTravelOffersByStatus")
fun getTravelOffersByStatus(
    @HttpTrigger(
        name = "req",
        methods = [HttpMethod.GET],
        route = "travelOffers/{status}"
    )
    request: HttpRequestMessage<Optional<String>>,
    @BindingName("status") statusParam: String,
    context: ExecutionContext
): HttpResponseMessage {

    val logger = context.logger
    logger.info("Received request for travel offers with status: $statusParam")

    val status = try {
        TravelOfferStatusEnum.valueOf(statusParam)
    } catch (ex: IllegalArgumentException) {
        return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
            .body("Invalid status parameter: $statusParam")
            .build()
    }

    val queryParams = request.queryParameters
    val page = queryParams["page"]?.toIntOrNull() ?: 1
    val size = queryParams["size"]?.toIntOrNull() ?: 10

    val response = runBlocking {
        travelOfferQuery.getTravelOffersByStatus(status, page, size)
    }

    return request.createResponseBuilder(HttpStatus.OK)
        .header("Content-Type", "application/json")
        .body(response)
        .build()
}

@FunctionName("BookTravelOffer")
fun bookTravelOffer(
    @HttpTrigger(
        name = "req",
        methods = [HttpMethod.POST],
        authLevel = AuthorizationLevel.ANONYMOUS,
        route = "bookTravelOffer"
    )
    request: HttpRequestMessage<Optional<String>>,
    context: ExecutionContext
): HttpResponseMessage = runBlocking {
    val (offerId, userId, seat) = extractQueryParams(request.queryParameters)
    val bookingId = bookingCommandHandler.handle(
        CreateBookingCommand(
            correlationId = UUID.randomUUID(),
            userId = userId,
            travelOfferId = offerId,
            seat = seat
        ) as BookingCommand
    ).bookingId

    return@runBlocking request.createResponseBuilder(HttpStatus.ACCEPTED)
        .header("Content-Type", "application/json")
        .body(mapOf("bookingId" to bookingId.toString()))
        .build()
}

@FunctionName("CancelTravelOfferBooking")
fun cancelTravelOfferBooking(
    @HttpTrigger(
        name = "req",
        methods = [HttpMethod.POST],
        authLevel = AuthorizationLevel.ANONYMOUS,
        route = "cancelTravelOffer"
    )
    request: HttpRequestMessage<Optional<String>>,
    context: ExecutionContext
): HttpResponseMessage = runBlocking {
    val bookingId = request.queryParameters["bookingId"]?.let { UUID.fromString(it) }

    requireNotNull(bookingId)
    val event = bookingCommandHandler.handle(
        BookingRequestCancelCommand(
            correlationId = UUID.randomUUID(),
            bookingId = bookingId,
        )
    )

    if (event is BookingCancelRequestedFailedEvent) {
        return@runBlocking request.createResponseBuilder(HttpStatus.FORBIDDEN)
            .header("Content-Type", "application/json")
            .body(event.message)
            .build()
    } else {
        return@runBlocking request.createResponseBuilder(HttpStatus.ACCEPTED)
            .header("Content-Type", "application/json")
            .body(mapOf("bookingId" to bookingId.toString()))
            .build()
    }
}

@FunctionName("getTravelOffers")
fun getTravelOffers(
    @HttpTrigger(
        name = "req",
        methods = [HttpMethod.GET],
        route = "travelOffers"
    ) request: HttpRequestMessage<Optional<String>>,
    context: ExecutionContext
): HttpResponseMessage {
    return try {
        val (page, size) = extractPaginationParams(request.queryParameters)
        val resp = runBlocking { travelOfferQuery.getTravelOffers(page, size) }

        request.createResponseBuilder(HttpStatus.OK)
            .header("Content-Type", "application/json")
            .body(resp)
            .build()
    } catch (e: Exception) {
        context.logger.severe("Error in getTravelOffers: ${e.message}")
        request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
            .body("Internal server error")
            .build()
    }
}

@FunctionName("CountTravelOffersByStatus")
fun countTravelOffersByStatus(
    @HttpTrigger(
        name = "req",
        methods = [HttpMethod.GET],
        route = "travelOffers/{status}/count"
    ) request: HttpRequestMessage<Optional<String>>,
    @BindingName("status") statusParam: String,
    context: ExecutionContext
): HttpResponseMessage {
    return try {
        val status = try {
            TravelOfferStatusEnum.valueOf(statusParam)
        } catch (e: IllegalArgumentException) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body("Invalid travel offer status $statusParam")
                .build()
        }
        context.logger.info("Received request for travel offers count with status: $status")
        val count = runBlocking {
            val container = CosmosClientProjectionProvider.getTravelOfferContainer()
            val querySpec = SqlQuerySpec("SELECT VALUE COUNT(1) FROM c")
            val result = container.queryItems(querySpec, CosmosQueryRequestOptions(), Long::class.java)
                .map { item ->
                    if (item != null) {
                        context.logger.info("RESULT : $item")
                    } else {
                        context.logger.info("RESULT : null")
                    }

                    item }
                .collectList().block()
            return@runBlocking result?.firstOrNull() ?: 0L
        }
        context.logger.info("IN C1 Found $count travel offers with status: $status")
//        val count = runBlocking { travelOfferQuery.countTravelOffersByStatus(status) }
//        context.logger.info("Found $count travel offers with status: $status")
        request.createResponseBuilder(HttpStatus.OK)
            .header("Content-Type", "application/json")
            .body(mapOf("count" to count))
            .build()
    } catch (e: Exception) {
        context.logger.severe("Error in countTravelOffersByStatus: ${e.message}")
        request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
            .body("Internal server error")
            .build()
    }
}

@FunctionName("getBookingById")
fun getBookingById(
    @HttpTrigger(
        name = "req",
        methods = [HttpMethod.GET],
        route = "booking/{bookingId}"
    ) request: HttpRequestMessage<Optional<String>>,
    @BindingName("bookingId") bookingIdParam: String,
    context: ExecutionContext
): HttpResponseMessage {
    return try {
        val bookingId = try {
            UUID.fromString(bookingIdParam)
        } catch (e: IllegalArgumentException) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body("Invalid booking ID format: $bookingIdParam")
                .build()
        }

        val booking = runBlocking { bookingQuery.getBookingById(bookingId) }

        request.createResponseBuilder(HttpStatus.OK)
            .header("Content-Type", "application/json")
            .body(BookingDto.fromDomain(booking))
            .build()
    } catch (e: Exception) {
        context.logger.severe("Error in getBookingById: ${e.message}")
        request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
            .body("Internal server error")
            .build()
    }
}

@FunctionName("getUserBookings")
fun getUserBookings(
    @HttpTrigger(
        name = "req",
        methods = [HttpMethod.GET],
        route = "userBookings/{userId}"
    ) request: HttpRequestMessage<Optional<String>>,
    @BindingName("userId") userIdParam: String,
    context: ExecutionContext
): HttpResponseMessage {
    return try {
        val (page, size) = extractPaginationParams(request.queryParameters)
        val userId = try {
            UUID.fromString(userIdParam)
        } catch (e: IllegalArgumentException) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body("Invalid user ID format: $userIdParam")
                .build()
        }

        val bookings = runBlocking { bookingQuery.getBookingsByUserId(page, size, userId) }
        val response = bookings.map { booking -> BookingDto.fromDomain(booking) }

        request.createResponseBuilder(HttpStatus.OK)
            .header("Content-Type", "application/json")
            .body(response)
            .build()
    } catch (e: Exception) {
        context.logger.severe("Error in getUserBookings: ${e.message}")
        request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
            .body("Internal server error")
            .build()
    }
}

@FunctionName("getTravelOffersUser")
fun getTravelOffersUser(
    @HttpTrigger(
        name = "req",
        methods = [HttpMethod.GET],
        route = "travelOffersUser/{userId}"
    ) request: HttpRequestMessage<Optional<String>>,
    @BindingName("userId") userIdParam: String,
    context: ExecutionContext
): HttpResponseMessage {
    return try {
        val (page, size) = extractPaginationParams(request.queryParameters)
        val userId = try {
            UUID.fromString(userIdParam)
        } catch (e: IllegalArgumentException) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body("Invalid user ID format: $userIdParam")
                .build()
        }

        val resp = runBlocking { bookingQuery.getTravelOffersByUserId(page, size, userId) }

        request.createResponseBuilder(HttpStatus.OK)
            .header("Content-Type", "application/json")
            .body(resp)
            .build()
    } catch (e: Exception) {
        context.logger.severe("Error in getTravelOffersUser: ${e.message}")
        request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
            .body("Internal server error")
            .build()
    }
}

@FunctionName("getTravelOffersByLocation")
fun getTravelOffersByLocation(
    @HttpTrigger(
        name = "req",
        methods = [HttpMethod.GET],
        route = "travelOffers/location/{location}"
    ) request: HttpRequestMessage<Optional<String>>,
    @BindingName("location") locationParam: String,
    context: ExecutionContext
): HttpResponseMessage {
    return try {
        val (page, size) = extractPaginationParams(request.queryParameters)

        val location = try {
            LocationEnum.valueOf(locationParam)
        } catch (e: IllegalArgumentException) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body("Invalid location: $locationParam")
                .build()
        }

        val status = request.queryParameters["status"]?.let {
            try {
                TravelOfferStatusEnum.valueOf(it)
            } catch (e: IllegalArgumentException) {
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Invalid travel offer status: $it")
                    .build()
            }
        } ?: return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
            .body("Missing required parameter: status")
            .build()

        val resp = runBlocking { travelOfferQuery.getTravelOfferByLocation(page, size, location, status) }

        request.createResponseBuilder(HttpStatus.OK)
            .header("Content-Type", "application/json")
            .body(resp)
            .build()
    } catch (e: Exception) {
        context.logger.severe("Error in getTravelOffersByLocation: ${e.message}")
        request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
            .body("Internal server error")
            .build()
    }
}

@FunctionName("countTravelOffersByLocation")
fun countTravelOffersByLocation(
    @HttpTrigger(
        name = "req",
        methods = [HttpMethod.GET],
        route = "travelOffers/location/{location}/count"
    ) request: HttpRequestMessage<Optional<String>>,
    @BindingName("location") locationParam: String,
    context: ExecutionContext
): HttpResponseMessage {
    return try {
        val location = try {
            LocationEnum.valueOf(locationParam)
        } catch (e: IllegalArgumentException) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body("Invalid location $locationParam")
                .build()
        }

        val status = request.queryParameters["status"]?.let {
            try {
                TravelOfferStatusEnum.valueOf(it)
            } catch (e: IllegalArgumentException) {
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Invalid travel offer status $it")
                    .build()
            }
        } ?: return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
            .body("Missing required parameter: status")
            .build()

        val count = runBlocking { travelOfferQuery.countTravelOffersByLocation(location, status) }

        request.createResponseBuilder(HttpStatus.OK)
            .header("Content-Type", "application/json")
            .body(mapOf("count" to count))
            .build()
    } catch (e: Exception) {
        context.logger.severe("Error in countTravelOffersByLocation: ${e.message}")
        request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
            .body("Internal server error")
            .build()
    }
}

@FunctionName("getTravelOfferById")
fun getTravelOfferById(
    @HttpTrigger(
        name = "req",
        methods = [HttpMethod.GET],
        route = "travelOffer/{id}"
    ) request: HttpRequestMessage<Optional<String>>,
    @BindingName("id") idParam: String,
    context: ExecutionContext
): HttpResponseMessage {
    return try {
        val travelOfferId = try {
            UUID.fromString(idParam)
        } catch (e: IllegalArgumentException) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body("Invalid travel offer Id: $idParam")
                .build()
        }

        val resp = runBlocking { travelOfferQuery.getTravelOfferById(travelOfferId) }

        request.createResponseBuilder(HttpStatus.OK)
            .header("Content-Type", "application/json")
            .body(resp)
            .build()
    } catch (e: Exception) {
        context.logger.severe("Error in getTravelOfferById: ${e.message}")
        request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
            .body("Internal server error")
            .build()
    }
}