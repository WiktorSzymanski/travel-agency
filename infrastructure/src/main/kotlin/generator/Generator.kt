package generator

interface Generator<T, R> {
    fun generate(filePath: String): List<R>

    fun toDomainModel(template: T): R
}
