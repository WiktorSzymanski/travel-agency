package pl.szymanski.wiktor.ta.infrastructure.generator

import pl.szymanski.wiktor.ta.domain.repository.Repository

class GeneratorRepoPair<T, R : Any>(
    val generator: Generator<T, R>,
    val repository: Repository<R>,
) {
    suspend fun process() {
        generator.generate().forEach { entity ->
            repository.save(entity)
        }
    }
}
