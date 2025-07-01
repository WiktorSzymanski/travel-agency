package pl.szymanski.wiktor.ta.domain.repository

interface Repository<T> {
    suspend fun save(entity: T): T
}
