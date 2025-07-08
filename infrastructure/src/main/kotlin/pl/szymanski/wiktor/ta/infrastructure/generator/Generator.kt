package pl.szymanski.wiktor.ta.infrastructure.generator

interface Generator<T, R> {
    fun generate(): List<R>

    fun toDomainModel(template: T): R
}
