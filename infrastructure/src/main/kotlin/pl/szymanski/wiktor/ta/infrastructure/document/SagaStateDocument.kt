package pl.szymanski.wiktor.ta.infrastructure.document

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.mapping.Document
import pl.szymanski.wiktor.ta.domain.AnySeat
import pl.szymanski.wiktor.ta.domain.PickedSeat
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import pl.szymanski.wiktor.ta.saga.SagaState
import pl.szymanski.wiktor.ta.saga.SagaStatus
import pl.szymanski.wiktor.ta.saga.SagaStep
import pl.szymanski.wiktor.ta.saga.SagaType
import java.util.UUID

@Document(collection = "sagas")
data class SagaStateDocument(
    @Id
    val id: UUID,
    val correlationId: String,
    val type: String,
    val status: String,
    val step: String,
    val travelOffer: TravelOfferDocument,
    val bookingId: String,
    val seat: SeatDocument?,
    val seatType: String,
    val message: String?,
    val retryCount: Int,
    @Version
    val version: Long = 0L,
) {
    companion object {
        fun fromDomain(saga: SagaState, version: Long = 0L): SagaStateDocument {
            val seatDoc: SeatDocument? = when (saga.seat) {
                is AnySeat -> null
                is PickedSeat -> SeatDocument((saga.seat as PickedSeat).row, (saga.seat as PickedSeat).column)
            }
            val seatType = when (saga.seat) {
                is AnySeat -> "AnySeat"
                is PickedSeat -> "PickedSeat"
            }
            return SagaStateDocument(
                id = saga.id,
                correlationId = saga.correlationId.toString(),
                type = saga.type.name,
                status = saga.status.name,
                step = saga.step.name,
                travelOffer = TravelOfferDocument.fromDomain(saga.travelOffer),
                bookingId = saga.bookingId.value?.toString() ?: "",
                seat = seatDoc,
                seatType = seatType,
                message = saga.message,
                retryCount = saga.retryCount,
                version = version,
            )
        }
    }

    fun toDomain(): SagaState {
        val domainSeat = when (seatType) {
            "AnySeat" -> AnySeat
            "PickedSeat" -> seat?.toDomain() ?: AnySeat
            else -> AnySeat
        }
        return SagaState(
            id = id,
            correlationId = UUID.fromString(correlationId),
            type = SagaType.valueOf(type),
            status = SagaStatus.valueOf(status),
            step = SagaStep.valueOf(step),
            travelOffer = travelOffer.toDomain(),
            bookingId = if (bookingId.isNotBlank()) BookingId.from(UUID.fromString(bookingId)) else BookingId.Empty,
            seat = domainSeat,
            message = message,
            retryCount = retryCount,
            version = version,
        )
    }
}



