package cz.cleevio.cache.dao

import androidx.room.Dao
import androidx.room.Query
import cz.cleevio.cache.entity.MyOfferEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MyOfferDao : BaseDao<MyOfferEntity> {

	@Query("SELECT * FROM MyOfferEntity")
	fun listAllFlow(): Flow<List<MyOfferEntity>>

	@Query("SELECT * FROM MyOfferEntity where extId = :offerId LIMIT 1")
	suspend fun getMyOfferById(offerId: String): MyOfferEntity?

	@Query("SELECT COUNT(id) FROM MyOfferEntity")
	suspend fun getMyOfferCount(): Int
}