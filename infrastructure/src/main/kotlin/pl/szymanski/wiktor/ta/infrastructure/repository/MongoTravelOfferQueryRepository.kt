package pl.szymanski.wiktor.ta.infrastructure.repository

import org.springframework.stereotype.Repository
import pl.szymanski.wiktor.ta.domain.TravelOfferStatusEnum
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOfferComponentId
import pl.szymanski.wiktor.ta.infrastructure.dto.TravelOfferDto
import pl.szymanski.wiktor.ta.infrastructure.config.MongoConfiguration
import pl.szymanski.wiktor.ta.queryrepository.ProjectionUpdate
import pl.szymanski.wiktor.ta.queryrepository.TravelOfferQueryRepository

@Repository
class MongoTravelOfferQueryRepository(
    mongoConfiguration: MongoConfiguration
) : TravelOfferQueryRepository {
    private val collection = mongoConfiguration.mongoClient()
        .getDatabase(mongoConfiguration.mongoConfig.dbName)
        .getCollection<TravelOffer>("travelOffers")

    override suspend fun save(entity: TravelOffer) {
        collection.insertOne(entity)
    }

    override suspend fun update(projectionUpdate: ProjectionUpdate) {
        throw UnsupportedOperationException("Not implemented in this approach")
    }

    override suspend fun findAllByStatus(status: TravelOfferStatusEnum): List<TravelOffer> {
        throw UnsupportedOperationException("Not implemented in this approach")
    }

    override suspend fun countTravelOffersByStatus(status: TravelOfferStatusEnum): Int {
        throw UnsupportedOperationException("Not implemented in this approach")
    }

    override suspend fun findByTravelOfferComponentId(id: TravelOfferComponentId): List<TravelOffer> {
        throw UnsupportedOperationException("Not implemented in this approach")
    }
}