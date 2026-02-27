package pl.szymanski.wiktor.ta.repository

import pl.szymanski.wiktor.ta.Metadata

interface CommandRepository<T, R> {
    suspend fun create(entity: T, metadata: Metadata)
    suspend fun save(entity: T, metadata: Metadata)
    suspend fun findById(id: R): Pair<T, Long>
}