package pl.szymanski.wiktor.ta.generator

import pl.szymanski.wiktor.ta.command.Command
import pl.szymanski.wiktor.ta.event.DateMetEvent
import java.time.Clock

abstract class TemplateGenerator<T, out R : Command, out E : DateMetEvent>(
    protected val inAdvanceSeconds: Long,
    protected val creationWindowSeconds: Long,
    protected val clock: Clock = Clock.systemDefaultZone()
) : Generator<T, R, E> {

    override fun generate(templates: List<T>): List<GeneratedResult<R, E>> {
        return templates.map { create(it) }
    }

    protected abstract fun create(template: T): GeneratedResult<R, E>
}
