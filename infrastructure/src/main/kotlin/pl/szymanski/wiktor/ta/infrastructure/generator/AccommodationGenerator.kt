package pl.szymanski.wiktor.ta.infrastructure.generator

import pl.szymanski.wiktor.ta.command.CreateAccommodationCommand
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.Rent
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

class AccommodationGenerator(
    private val inAdvanceSeconds: Long,
    private val creationWindowSeconds: Long,
    private val templates: List<AccommodationTemplate>,
    private val clock: Clock = Clock.systemDefaultZone(),
) : Generator<AccommodationTemplate, CreateAccommodationCommand> {
    override fun generate(): List<CreateAccommodationCommand> = templates.map { toCommand(it) }

    override fun toCommand(template: AccommodationTemplate): CreateAccommodationCommand {
        val fromTime =
            randomDateTimeBetween(
                LocalDateTime.now(clock).plusSeconds(inAdvanceSeconds),
                LocalDateTime.now(clock).plusSeconds(inAdvanceSeconds + creationWindowSeconds / 2),
            )

        val tillTime =
            randomDateTimeBetween(
                fromTime,
                LocalDateTime.now(clock).plusSeconds(inAdvanceSeconds + creationWindowSeconds),
            )

        return CreateAccommodationCommand(
            name = template.name,
            location = LocationEnum.valueOf(template.location.uppercase()),
            rent = Rent(
                from = fromTime,
                till = tillTime,
            ),
            accommodationId = UUID.randomUUID(),
            correlationId = UUID.randomUUID(),
        )
    }
}
