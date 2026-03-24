package pl.szymanski.wiktor.ta.infrastructure.projection

import io.kurrent.dbclient.ResolvedEvent
import pl.szymanski.wiktor.ta.Metadata

interface ProjectionHandler {
    fun canHandle(streamName: String): Boolean
    suspend fun handle(event: ResolvedEvent, metadata: Metadata)
}
