package pl.szymanski.wiktor.ta.generator

import pl.szymanski.wiktor.ta.commands.commute.create.CreateCommuteCommand
import pl.szymanski.wiktor.ta.domain.LocationAndTime
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.aggregate.CommuteId
import pl.szymanski.wiktor.ta.event.CommuteDateMetEvent
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

class CommuteGenerator(
    inAdvanceSeconds: Long,
    creationWindowSeconds: Long,
    templates: List<CommuteTemplate>,
    clock: Clock = Clock.systemDefaultZone(),
) : TemplateGenerator<CommuteTemplate, CreateCommuteCommand, CommuteDateMetEvent>(
    inAdvanceSeconds,
    creationWindowSeconds,
    templates,
    clock
) {
    override fun create(template: CommuteTemplate): GeneratedResult<CreateCommuteCommand, CommuteDateMetEvent> {
        val departureTime = randomDateTimeBetween(
            LocalDateTime.now(clock).plusSeconds(inAdvanceSeconds),
            LocalDateTime.now(clock).plusSeconds(inAdvanceSeconds + creationWindowSeconds / 2),
        )
        val arrivalTime = randomDateTimeBetween(
            departureTime,
            departureTime.plusSeconds(creationWindowSeconds),
        )

        val id = CommuteId.generate()
        val command = CreateCommuteCommand(
            commuteId = id,
            correlationId = UUID.randomUUID(),
            name = template.name,
            departure = LocationAndTime(
                location = LocationEnum.valueOf(template.departureLocation.uppercase()),
                time = departureTime,
            ),
            arrival = LocationAndTime(
                location = LocationEnum.valueOf(template.arrivalLocation.uppercase()),
                time = arrivalTime,
            ),
            seats = template.seats,
        )
        val event = CommuteDateMetEvent(commuteId = id.value!!, date = departureTime)
        return GeneratedResult(command, event, departureTime)
    }
}
