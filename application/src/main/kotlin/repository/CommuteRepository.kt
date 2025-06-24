package repository

import aggregate.Commute
import java.util.UUID

interface CommuteRepository {
    fun findById(commuteId: UUID): Commute?
    fun save(commute: Commute)
}