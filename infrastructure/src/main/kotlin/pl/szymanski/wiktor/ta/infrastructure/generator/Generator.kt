package pl.szymanski.wiktor.ta.infrastructure.generator

interface Generator<T, R> {
    fun generate(): List<R>

    fun toCommand(template: T): R
}
