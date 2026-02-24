package pl.szymanski.wiktor.ta.commands.commute.create

import pl.szymanski.wiktor.ta.commands.Command
import pl.szymanski.wiktor.ta.domain.LocationAndTime
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.aggregate.CommuteId
import java.util.UUID

data class CreateCommuteCommand(
    override val correlationId: UUID,
    val commuteId: CommuteId,
    val name: String,
    val departure: LocationAndTime,
    val arrival: LocationAndTime,
    val seats: List<Seat>,
) : Command
