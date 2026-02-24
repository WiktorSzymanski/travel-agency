package pl.szymanski.wiktor.ta.commands

import java.util.UUID

interface Command {
    val correlationId: UUID
}
