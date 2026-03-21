package pl.szymanski.wiktor.ta.generator

import pl.szymanski.wiktor.ta.commands.Command
import pl.szymanski.wiktor.ta.event.DateMetEvent
import java.time.LocalDateTime

data class GeneratedResult<out R : Command, out E : DateMetEvent>(
    val command: R,
    val event: E,
    val scheduleDate: LocalDateTime
)

interface Generator<T, out R : Command, out E : DateMetEvent> {
    fun generate(): List<GeneratedResult<R, E>>
}
