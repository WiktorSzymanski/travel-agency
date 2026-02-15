package pl.szymanski.wiktor.ta

import pl.szymanski.wiktor.ta.command.Command
import pl.szymanski.wiktor.ta.domain.event.DomainEvent

fun interface CommandHandler<C : Command, E> {
    suspend fun handle(command: C): Triple<E, List<DomainEvent>, Metadata>
}

interface CommandBus {
    fun <C : Command, E> registerHandler(commandType: Class<C>, handler: CommandHandler<C, E>)
    suspend fun <C : Command, E> dispatch(command: C): Triple<E, List<DomainEvent>, Metadata>
    suspend fun dispatchAndForget(command: Command)
}
