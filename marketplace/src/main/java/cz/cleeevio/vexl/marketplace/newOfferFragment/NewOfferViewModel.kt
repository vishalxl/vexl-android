package cz.cleeevio.vexl.marketplace.newOfferFragment

import androidx.lifecycle.viewModelScope
import com.cleevio.vexl.cryptography.KeyPairCryptoLib
import cz.cleevio.cache.preferences.EncryptedPreferenceRepository
import cz.cleevio.core.model.OfferParams
import cz.cleevio.core.utils.LocationHelper
import cz.cleevio.core.utils.OfferUtils
import cz.cleevio.core.widget.FriendLevel
import cz.cleevio.network.data.Resource
import cz.cleevio.network.data.Status
import cz.cleevio.repository.model.offer.NewOffer
import cz.cleevio.repository.model.offer.Offer
import cz.cleevio.repository.repository.chat.ChatRepository
import cz.cleevio.repository.repository.contact.ContactRepository
import cz.cleevio.repository.repository.offer.OfferRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import lightbase.core.baseClasses.BaseViewModel


class NewOfferViewModel constructor(
	private val contactRepository: ContactRepository,
	private val offerRepository: OfferRepository,
	private val chatRepository: ChatRepository,
	private val encryptedPreferenceRepository: EncryptedPreferenceRepository,
	private val locationHelper: LocationHelper
) : BaseViewModel() {

	private val _newOfferRequest = MutableSharedFlow<Resource<Offer>>()
	val newOfferRequest = _newOfferRequest.asSharedFlow()

	fun createOffer(params: OfferParams) {
		viewModelScope.launch(Dispatchers.IO) {

			_newOfferRequest.emit(Resource.loading())
			val encryptedOfferList: MutableList<NewOffer> = mutableListOf()

			//load all public keys for specified level of friends
			val contactsPublicKeys = when (params.friendLevel.value) {
				FriendLevel.NONE -> emptyList()
				FriendLevel.FIRST_DEGREE -> contactRepository.getFirstLevelContactKeys()
				FriendLevel.SECOND_DEGREE -> contactRepository.getContactKeys()
				else -> emptyList()
			}.map {
				it.key // get just the keys
			}.toMutableSet() // remove duplicities

			//also add user's key
			encryptedPreferenceRepository.userPublicKey.let { myPublicKey ->
				contactsPublicKeys.add(myPublicKey)
			}

			val offerKeys = KeyPairCryptoLib.generateKeyPair()
			//encrypt in loop for every contact
			contactsPublicKeys.forEach { key ->
				val encryptedOffer = OfferUtils.encryptOffer(locationHelper, params, key, offerKeys)
				encryptedOfferList.add(encryptedOffer)
			}

			//send all in single request to BE
			val response = offerRepository.createOffer(encryptedOfferList, params.expiration)
			when (response.status) {
				is Status.Success -> {
					//save offer ID into DB, also save keys
					val offerData = response.data
					offerData?.let { offer ->
						offerRepository.saveMyOfferIdAndKeys(
							offerId = offer.offerId,
							privateKey = offerKeys.privateKey,
							publicKey = offerKeys.publicKey,
							offerType = offer.offerType,
							isInboxCreated = false
						)
					}

					val inboxResponse = chatRepository.createInbox(offerKeys.publicKey)
					when (inboxResponse.status) {
						is Status.Success -> {
							offerData?.let { offer ->
								offerRepository.saveMyOfferIdAndKeys(
									offerId = offer.offerId,
									privateKey = offerKeys.privateKey,
									publicKey = offerKeys.publicKey,
									offerType = offer.offerType,
									isInboxCreated = true
								)
							}
							_newOfferRequest.emit(response)
						}
						is Status.Error -> {
							//do we need other flow for errors?
							_newOfferRequest.emit(Resource.error(inboxResponse.errorIdentification))
						}
					}
				}
				is Status.Error -> {
					_newOfferRequest.emit(Resource.error(response.errorIdentification))
				}
			}
		}
	}
}