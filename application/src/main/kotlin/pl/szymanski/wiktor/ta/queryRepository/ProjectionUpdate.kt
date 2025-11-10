package pl.szymanski.wiktor.ta.queryRepository

import java.util.UUID

data class ProjectionUpdate(
    val id: UUID,
    val updates: List<UpdateRecord<Any>>,
)

data class UpdateRecord<T>(
    val field: String,
    val value: T
)