package pl.szymanski.wiktor.ta.generator

import pl.szymanski.wiktor.ta.commands.Command
import pl.szymanski.wiktor.ta.event.DateMetEvent
import java.time.Clock

abstract class TemplateGenerator<T, out R : Command, out E : DateMetEvent>(
    protected val inAdvanceSeconds: Long,
    protected val creationWindowSeconds: Long,
    protected val templates: List<T>,
    protected val clock: Clock = Clock.systemDefaultZone()
) : Generator<T, R, E> {

    override fun generate(): List<GeneratedResult<R, E>> {
        return templates.map { create(it) }
    }

    protected abstract fun create(template: T): GeneratedResult<R, E>
}
