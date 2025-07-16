package pl.szymanski.wiktor.ta.command

import java.util.UUID

interface Command {
    val correlationId: UUID
}