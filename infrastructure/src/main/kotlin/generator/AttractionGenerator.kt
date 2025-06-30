package generator

import loader.AttractionTemplate
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction

class AttractionGenerator : Generator<AttractionTemplate, Attraction> {
    override fun generate(filePath: String): List<Attraction> {
        TODO("Not yet implemented")
    }

    override fun toDomainModel(template: AttractionTemplate): Attraction {
        TODO("Not yet implemented")
    }
}
