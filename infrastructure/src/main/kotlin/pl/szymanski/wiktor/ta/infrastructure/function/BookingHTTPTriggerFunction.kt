package pl.szymanski.wiktor.ta.infrastructure.function

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import kotlinx.coroutines.runBlocking
import pl.szymanski.wiktor.ta.command.BookingCommand
import pl.szymanski.wiktor.ta.command.CreateBookingCommand
import pl.szymanski.wiktor.ta.commandHandler.BookingCommandHandler
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.infrastructure.repository.command.BookingRepositoryImpl
import java.util.Optional
import java.util.UUID

val bookingCommandHandler = BookingCommandHandler(
    bookingRepository = BookingRepositoryImpl()
)

fun extractQueryParams(queryParams: Map<String, String>): Triple<UUID, UUID, Seat?> {
    val offerId = queryParams["offerId"]?.let { UUID.fromString(it) }
    val userId = queryParams["userId"]?.let { UUID.fromString(it) }
    val seat = queryParams["seat"]?.let { Seat.fromString(it) }

    requireNotNull(offerId)
    requireNotNull(userId)

    return Triple(offerId, userId, seat)
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