package pl.szymanski.wiktor.ta.commands

interface CommandHandler<C : Command> {
    suspend fun handle(command: C)
}