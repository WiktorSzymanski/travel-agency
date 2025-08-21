package pl.szymanski.wiktor.ta.queryRepository

import pl.szymanski.wiktor.ta.domain.AccommodationStatusEnum
import pl.szymanski.wiktor.ta.domain.event.Event
import java.util.UUID

data class AccommodationUpdateRevision(
    val _id: UUID,
    val revision: Int,
    val event: Event
)

data class AccommodationUpdate(
    val _id: UUID,
    val status: AccommodationStatusEnum? = null,
    val bookingId: UUID? = null,
    val revision: Int,
)

data class AccommodationUpdateStatus(
    val _id: UUID,
    val status: AccommodationStatusEnum? = null,
    val revision: Int,
)