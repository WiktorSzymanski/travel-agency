package pl.szymanski.wiktor.ta.queryRepository

import pl.szymanski.wiktor.ta.domain.AccommodationStatusEnum
import java.util.UUID

data class AccommodationUpdate(
    val _id: UUID,
    val status: AccommodationStatusEnum? = null,
    val bookingId: UUID? = null
)

data class AccommodationUpdateStatus(
    val _id: UUID,
    val status: AccommodationStatusEnum? = null,
)