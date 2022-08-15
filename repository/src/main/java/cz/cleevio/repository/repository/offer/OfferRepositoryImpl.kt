package cz.cleevio.repository.repository.offer

import androidx.sqlite.db.SimpleSQLiteQuery
import com.cleevio.vexl.cryptography.model.KeyPair
import cz.cleevio.cache.TransactionProvider
import cz.cleevio.cache.dao.*
import cz.cleevio.cache.entity.MyOfferEntity
import cz.cleevio.cache.entity.OfferCommonFriendCrossRef
import cz.cleevio.network.LocationApi
import cz.cleevio.network.api.OfferApi
import cz.cleevio.network.data.Resource
import cz.cleevio.network.data.Status
import cz.cleevio.network.extensions.tryOnline
import cz.cleevio.network.request.offer.CreateOfferRequest
import cz.cleevio.network.request.offer.DeletePrivatePartRequest
import cz.cleevio.network.request.offer.UpdateOfferRequest
import cz.cleevio.repository.model.contact.fromDao
import cz.cleevio.repository.model.offer.*
import cz.cleevio.repository.repository.chat.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal

class OfferRepositoryImpl constructor(
	private val offerApi: OfferApi,
	private val locationApi: LocationApi,
	private val myOfferDao: MyOfferDao,
	private val offerDao: OfferDao,
	private val requestedOfferDao: RequestedOfferDao,
	private val locationDao: LocationDao,
	private val contactDao: ContactDao,
	private val offerCommonFriendCrossRefDao: OfferCommonFriendCrossRefDao,
	private val transactionProvider: TransactionProvider,
	private val chatRepository: ChatRepository,
) : OfferRepository {

	override val buyOfferFilter = MutableStateFlow(OfferFilter())

	override val sellOfferFilter = MutableStateFlow(OfferFilter())

	override suspend fun createOffer(offerList: List<NewOffer>, expiration: Long, offerKeys: KeyPair): Resource<Offer> {
		//create offer
		val offerCreateResource = tryOnline(
			request = {
				offerApi.postOffers(
					CreateOfferRequest(
						offerPrivateList = offerList.map { it.toNetwork() },
						expiration = expiration
					)
				)
			},
			mapper = { it?.fromNetwork() }
		)

		offerCreateResource.data?.let { offer ->
			//save keys and info into my offers
			saveMyOfferIdAndKeys(
				offerId = offer.offerId,
				privateKey = offerKeys.privateKey,
				publicKey = offerKeys.publicKey,
				offerType = offer.offerType,
				isInboxCreated = false
			)

			//create inbox
			val inboxResponse = chatRepository.createInbox(offerKeys.publicKey)
			when (inboxResponse.status) {
				is Status.Success -> {
					//update info in my offers
					saveMyOfferIdAndKeys(
						offerId = offer.offerId,
						privateKey = offerKeys.privateKey,
						publicKey = offerKeys.publicKey,
						offerType = offer.offerType,
						isInboxCreated = true
					)
				}
				is Status.Error -> {
					//do we need other flow for errors?
					return Resource.error(inboxResponse.errorIdentification)
				}
			}

			//save offer into DB
			updateOffers(listOf(offer))
		}

		return offerCreateResource
	}

	override suspend fun updateOffer(offerId: String, offerList: List<NewOffer>): Resource<Offer> = tryOnline(
		request = {
			offerApi.putOffers(
				UpdateOfferRequest(
					offerId = offerId,
					offerPrivateCreateList = offerList.map { it.toNetwork() }
				)
			)
		},
		mapper = { it?.fromNetwork() },
		doOnSuccess = {
			it?.let { offer ->
				updateOffers(listOf(offer))
			}
		}
	)

	override suspend fun loadOffersForMe(): Resource<List<Offer>> = tryOnline(
		request = {
			offerApi.getOffersMe()
		},
		mapper = { it?.items?.map { item -> item.fromNetwork() } }
	)

	override suspend fun deleteMyOffers(offerIds: List<String>): Resource<Unit> = tryOnline(
		request = {
			offerApi.deleteOffersId(offerIds = offerIds)
		},
		mapper = { },
		doOnSuccess = {
			offerDao.deleteOffersById(offerIds)
			myOfferDao.deleteMyOffersById(offerIds)
		}
	)

	override suspend fun deleteOfferById(offerId: String): Resource<Unit> = deleteMyOffers(
		listOf(offerId)
	)

	override suspend fun deleteOfferForPublicKeys(deletePrivatePartRequest: DeletePrivatePartRequest)
		: Resource<Unit> = tryOnline(
		request = {
			offerApi.deleteOffersPrivatePart(
				deletePrivatePartRequest = deletePrivatePartRequest
			)
		},
		mapper = { },
		doOnSuccess = {
			syncOffers()
		}
	)

	override suspend fun refreshOffer(offerId: String): Resource<List<Offer>> = tryOnline(
		request = { offerApi.getOffersId(listOf(offerId)) },
		mapper = { items -> items?.map { item -> item.fromNetwork() } ?: emptyList() },
		doOnSuccess = {
			it?.let { offers ->
				updateOffers(offers)
			}
		}
	)

	override suspend fun saveMyOfferIdAndKeys(
		offerId: String,
		privateKey: String,
		publicKey: String,
		offerType: String,
		isInboxCreated: Boolean
	): Resource<Unit> {
		myOfferDao.replace(
			MyOfferEntity(
				extId = offerId,
				privateKey = privateKey,
				publicKey = publicKey,
				offerType = offerType,
				isInboxCreated = isInboxCreated
			)
		)
		return Resource.success(data = Unit)
	}

	override suspend fun loadOfferKeysByOfferId(offerId: String): KeyPair? {
		val entity = myOfferDao.getMyOfferById(offerId)
		return entity?.let {
			KeyPair(privateKey = entity.privateKey, publicKey = entity.publicKey)
		}
	}

	override suspend fun getOffers() =
		offerDao.getAllExtendedOffers().map {
			it.offer.fromCache(it.locations, it.commonFriends)
		}

	override fun getOffersFlow() = offerDao.getAllExtendedOffersFlow().map { list ->
		list.map {
			it.offer.fromCache(it.locations, it.commonFriends)
		}
	}

	override fun getFilteredAndSortedOffersByTypeFlow(
		offerTypeName: String,
		offerFilter: OfferFilter
	): Flow<List<Offer>> {
		val queryBuilder = StringBuilder("")
		val values = arrayListOf<Any>()

		queryBuilder.append("SELECT * FROM OfferEntity WHERE offerType == $SQL_VALUE_PLACEHOLDER")
		values.add(offerTypeName)
		queryBuilder.append(" AND active == $SQL_VALUE_PLACEHOLDER")
		values.add(true)

		if (offerFilter.locationType != null) {
			queryBuilder.append(" AND locationState == $SQL_VALUE_PLACEHOLDER")
			values.add(offerFilter.locationType)
		}
		if (offerFilter.paymentMethods?.isNotEmpty() == true) {
			val paymentMethods = offerFilter.paymentMethods.toList().joinToString(
				separator = SQL_VALUE_SEPARATOR,
				transform = { SQL_VALUE_PLACEHOLDER }
			)
			queryBuilder.append(" AND paymentMethod in($paymentMethods)")
			values.addAll(offerFilter.paymentMethods)
		}
		if (offerFilter.btcNetworks?.isNotEmpty() == true) {
			val networks = offerFilter.btcNetworks.toList().joinToString(
				separator = SQL_VALUE_SEPARATOR,
				transform = { SQL_VALUE_PLACEHOLDER }
			)
			queryBuilder.append(" AND btcNetwork in($networks)")
			values.addAll(offerFilter.btcNetworks)
		}
		if (offerFilter.friendLevels?.isNotEmpty() == true) {
			val levels = offerFilter.friendLevels.joinToString(
				separator = SQL_VALUE_SEPARATOR,
				transform = { SQL_VALUE_PLACEHOLDER }
			)
			queryBuilder.append(" AND friendLevel in($levels)")
			values.addAll(offerFilter.friendLevels)
		}
		if (offerFilter.feeType != null) {
			queryBuilder.append(" AND feeState == $SQL_VALUE_PLACEHOLDER")
			values.add(offerFilter.feeType)
		}
		if (offerFilter.feeValue != null) {
			queryBuilder.append(" AND feeAmount == $SQL_VALUE_PLACEHOLDER")
			values.add(offerFilter.feeValue)
		}
		if (offerFilter.currency != null) {
			queryBuilder.append(" AND currency == $SQL_VALUE_PLACEHOLDER")
			values.add(offerFilter.currency)
		}
		/* 	FIXME this query is not working if offer price range match second or other nested conditions then is the first one
			It is filtered programatically at the bottom of this method
			Maybe change query from AND/OR to BETWEEN

		if (offerFilter.priceRangeBottomLimit != null && offerFilter.priceRangeTopLimit != null) {
			queryBuilder.append(" AND (")
			queryBuilder.append(" (amountTopLimit >= $SQL_VALUE_PLACEHOLDER AND amountTopLimit <= $SQL_VALUE_PLACEHOLDER)")
			queryBuilder.append(" OR (amountBottomLimit >= $SQL_VALUE_PLACEHOLDER AND amountBottomLimit <= $SQL_VALUE_PLACEHOLDER)")
			queryBuilder.append(" OR ($SQL_VALUE_PLACEHOLDER >= amountBottomLimit AND $SQL_VALUE_PLACEHOLDER <= amountTopLimit)")
			queryBuilder.append(" OR ($SQL_VALUE_PLACEHOLDER >= amountBottomLimit AND $SQL_VALUE_PLACEHOLDER <= amountTopLimit)")
			queryBuilder.append(" )")

			values.add(offerFilter.priceRangeBottomLimit)
			values.add(offerFilter.priceRangeTopLimit)
			values.add(offerFilter.priceRangeBottomLimit)
			values.add(offerFilter.priceRangeTopLimit)
			values.add(offerFilter.priceRangeBottomLimit)
			values.add(offerFilter.priceRangeBottomLimit)
			values.add(offerFilter.priceRangeTopLimit)
			values.add(offerFilter.priceRangeTopLimit)
		}*/

		queryBuilder.append(" AND isMine == false")
		queryBuilder.append(" ORDER BY createdAt DESC, isRequested ASC")
		val simpleSQLiteQuery = SimpleSQLiteQuery(
			queryBuilder.toString(),
			values.toTypedArray()
		)

		return offerDao.getFilteredOffersFlow(simpleSQLiteQuery).map { list ->
			list.map {
				it.offer.fromCache(it.locations, it.commonFriends)
			}.filter { offer ->
				offerFilter.isOfferMatchPriceRange(offer.amountBottomLimit.toFloat(), offer.amountTopLimit.toFloat())
					&& offerFilter.isOfferLocationInRadius(offer.location)
			}
		}
	}

	override fun getOffersSortedByDateOfCreationFlow(offerTypeName: String): Flow<List<Offer>> {
		val queryBuilder = StringBuilder("")
		val values = arrayListOf<Any>()

		queryBuilder.append("SELECT * FROM OfferEntity ORDER BY createdAt DESC, isRequested ASC")

		val simpleSQLiteQuery = SimpleSQLiteQuery(
			queryBuilder.toString(),
			values.toTypedArray()
		)

		return offerDao.getFilteredOffersFlow(simpleSQLiteQuery).map { list ->
			list.map {
				it.offer.fromCache(it.locations, it.commonFriends)
			}
		}.map { list ->
			list.filter { it.offerType == offerTypeName && it.isMine }
		}
	}


	override suspend fun syncOffers() {
		val newOffers = getNewOffers()
		if (newOffers.isSuccess()) overwriteOffers(newOffers.data.orEmpty())
	}

	override suspend fun getMyOffersCount(offerType: String): Int =
		myOfferDao.getMyOfferCount(offerType)

	private suspend fun overwriteOffers(offers: List<Offer>) {
		val myOfferIds = myOfferDao.listAll().map { it.extId }
		val requestedOffersIds = requestedOfferDao.listAll().map { it.offerId }
		transactionProvider.runAsTransaction {
			offerCommonFriendCrossRefDao.clearTable()
			locationDao.clearTable()
			offerDao.clearTable()
			offers.forEach { offer ->
				offer.isMine = myOfferIds.contains(offer.offerId)
				offer.isRequested = requestedOffersIds.contains(offer.offerId)
				val offerId = offerDao.insertOffer(offer.toCache())
				locationDao.insertLocations(offer.location.map {
					it.toCache(offerId)
				})
				val commonFriendRefs = contactDao.getAllContacts()
					.map { it.fromDao() }
					.filter { contact ->
						offer.commonFriends.firstOrNull { it.contactHash == contact.getHashedContact() } != null
					}
					.map {
						OfferCommonFriendCrossRef(
							offerId = offerId,
							contactId = it.id
						)
					}
				offerCommonFriendCrossRefDao.replaceAll(commonFriendRefs)
			}
		}
	}

	private suspend fun updateOffers(offers: List<Offer>) {
		val myOfferIds = myOfferDao.listAll().map { it.extId }
		transactionProvider.runAsTransaction {
			offers.forEach { offer ->
				offer.isMine = myOfferIds.contains(offer.offerId)
				val offerId = offerDao.insertOffer(offer.toCache())
				locationDao.insertLocations(offer.location.map {
					it.toCache(offerId)
				})
				// because we don't have full objects at this moment yet (it's from the API, not database)
				val commonFriendRefs = contactDao.getAllContacts()
					.map { it.fromDao() }
					.filter { contact ->
						offer.commonFriends.firstOrNull { it.contactHash == contact.getHashedContact() } != null
					}
					.map {
						OfferCommonFriendCrossRef(
							offerId = offerId,
							contactId = it.id
						)
					}
				offerCommonFriendCrossRefDao.replaceAll(commonFriendRefs)
			}
		}
	}

	private suspend fun getNewOffers(): Resource<List<Offer>> = tryOnline(
		request = {
			offerApi.getModifiedOffers(0, Int.MAX_VALUE, "1970-01-01T00:00:00.000Z")
		},
		mapper = { it?.items?.map { item -> item.fromNetwork() } }
	)

	override suspend fun getMyOffersWithoutInbox(): List<MyOffer> =
		myOfferDao.getMyOffersWithoutInbox()
			.map { it.fromCache() }

	override suspend fun getLocationSuggestions(
		count: Int, query: String, language: String
	): Resource<List<LocationSuggestion>> = tryOnline(
		request = {
			locationApi.getSuggestions(count, query, language)
		},
		mapper = {
			it?.result?.map { a ->
				LocationSuggestion(
					a.userData.municipality,
					a.userData.region,
					a.userData.country,
					BigDecimal(a.userData.latitude),
					BigDecimal(a.userData.longitude)
				)
			}
				?.filter { a -> a.city.isNotBlank() }
				?.distinctBy { a -> a.city }
				?.filter { a -> a.city.contains(query, ignoreCase = true) }
				.orEmpty()
		}
	)

	override suspend fun clearOfferTables() {
		requestedOfferDao.clearTable()
		offerDao.clearTable()
		offerCommonFriendCrossRefDao.clearTable()
		myOfferDao.clearTable()
		locationDao.clearTable()
		contactDao.clearTable()
	}

	companion object {
		private const val SQL_VALUE_SEPARATOR = ","
		private const val SQL_VALUE_PLACEHOLDER = "?"
	}
}
