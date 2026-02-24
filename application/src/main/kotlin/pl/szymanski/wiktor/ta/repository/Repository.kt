package pl.szymanski.wiktor.ta.repository

import pl.szymanski.wiktor.ta.Metadata

interface Repository<T> {
    suspend fun create(entity: T, metadata: Metadata)
    suspend fun save(entity: T, metadata: Metadata)
}