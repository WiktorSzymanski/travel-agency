package pl.szymanski.wiktor.ta.generator

import pl.szymanski.wiktor.ta.command.CreateAccommodationCommand
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.Rent
import pl.szymanski.wiktor.ta.domain.aggregate.AccommodationId
import pl.szymanski.wiktor.ta.event.AccommodationDateMetEvent
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

class AccommodationGenerator(
    inAdvanceSeconds: Long,
    creationWindowSeconds: Long,
    clock: Clock = Clock.systemDefaultZone(),
) : TemplateGenerator<AccommodationTemplate, CreateAccommodationCommand, AccommodationDateMetEvent>(
    inAdvanceSeconds,
    creationWindowSeconds,
    clock
) {

    override fun create(template: AccommodationTemplate): GeneratedResult<CreateAccommodationCommand, AccommodationDateMetEvent> {
        val fromTime = randomDateTimeBetween(
            LocalDateTime.now(clock).plusSeconds(inAdvanceSeconds),
            LocalDateTime.now(clock).plusSeconds(inAdvanceSeconds + creationWindowSeconds / 2),
        )
        val tillTime = randomDateTimeBetween(
            fromTime,
            LocalDateTime.now(clock).plusSeconds(inAdvanceSeconds + creationWindowSeconds),
        )

        val id = AccommodationId.generate()
        val command = CreateAccommodationCommand(
            accommodationId = id,
            correlationId = UUID.randomUUID(),
            name = template.name,
            location = LocationEnum.valueOf(template.location.uppercase()),
            rent = Rent(
                from = fromTime,
                till = tillTime,
            )
        )
        val event = AccommodationDateMetEvent(accommodationId = id)
        return GeneratedResult(command, event, fromTime)
    }
}
