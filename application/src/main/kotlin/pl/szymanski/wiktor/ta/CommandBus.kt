package pl.szymanski.wiktor.ta

import pl.szymanski.wiktor.ta.command.Command
import pl.szymanski.wiktor.ta.domain.event.DomainEvent

fun interface CommandHandler<C : Command, E> {
    suspend fun handle(command: C): Pair<E, List<DomainEvent>>
}

interface CommandBus {
    fun <C : Command, E> registerHandler(commandType: Class<C>, handler: CommandHandler<C, E>)
    suspend fun <C : Command, E> dispatch(command: C): Pair<E, List<DomainEvent>>
}
