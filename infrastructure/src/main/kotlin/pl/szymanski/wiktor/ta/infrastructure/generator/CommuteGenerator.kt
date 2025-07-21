package pl.szymanski.wiktor.ta.infrastructure.generator

import pl.szymanski.wiktor.ta.command.commute.CreateCommuteCommand
import pl.szymanski.wiktor.ta.domain.LocationAndTime
import pl.szymanski.wiktor.ta.domain.LocationEnum
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

class CommuteGenerator(
    val inAdvanceSeconds: Long,
    val creationWindowSeconds: Long,
    val templates: List<CommuteTemplate>,
    val clock: Clock = Clock.systemDefaultZone(),
) : Generator<CommuteTemplate, CreateCommuteCommand> {
    override fun generate(): List<CreateCommuteCommand> = templates.map { toCommand(it) }

    override fun toCommand(template: CommuteTemplate): CreateCommuteCommand {
        val dTime =
            randomDateTimeBetween(
                LocalDateTime.now(clock).plusSeconds(inAdvanceSeconds),
                LocalDateTime.now(clock).plusSeconds(inAdvanceSeconds + creationWindowSeconds / 2),
            )

        val aTime =
            randomDateTimeBetween(
                dTime,
                LocalDateTime.now(clock).plusSeconds(inAdvanceSeconds + creationWindowSeconds),
            )

        return CreateCommuteCommand(
            name = template.name,
            departure = LocationAndTime(
                LocationEnum.valueOf(template.departureLocation.uppercase()),
                dTime,
            ),
            arrival = LocationAndTime(
                LocationEnum.valueOf(template.arrivalLocation.uppercase()),
                aTime,
            ),
            seats = template.seats,
            commuteId = UUID.randomUUID(),
            correlationId = UUID.randomUUID(),
        )
    }
}
