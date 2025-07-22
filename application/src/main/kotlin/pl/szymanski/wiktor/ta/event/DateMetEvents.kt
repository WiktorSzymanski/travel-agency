package pl.szymanski.wiktor.ta.event

import java.util.UUID

data class CommuteDateMetEvent(val commuteId: UUID, val correlationId: UUID)

data class AccommodationDateMetEvent(val accommodationId: UUID, val correlationId: UUID)

 class AttractionDateMetEvent(val attractionId: UUID, val correlationId: UUID)
