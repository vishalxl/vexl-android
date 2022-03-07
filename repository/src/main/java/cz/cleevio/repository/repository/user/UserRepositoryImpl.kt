package cz.cleevio.repository.repository.user

import cz.cleevio.cache.dao.UserDao
import cz.cleevio.cache.entity.User
import cz.cleevio.cache.preferences.EncryptedPreferenceRepository
import cz.cleevio.network.api.UserApi
import cz.cleevio.network.data.Resource
import cz.cleevio.network.extensions.tryOnline
import cz.cleevio.network.request.user.ConfirmCodeRequest
import cz.cleevio.network.request.user.ConfirmPhoneRequest
import cz.cleevio.network.response.user.ConfirmCodeResponse
import cz.cleevio.network.request.user.UsernameAvailableRequest
import cz.cleevio.network.response.user.ConfirmPhoneResponse
import cz.cleevio.repository.model.UserProfile
import cz.cleevio.repository.model.user.ConfirmCode
import cz.cleevio.repository.model.user.ConfirmPhone
import cz.cleevio.repository.model.user.UsernameAvailable
import cz.cleevio.repository.model.user.fromNetwork
import kotlinx.coroutines.flow.Flow

class UserRepositoryImpl constructor(
	private val userRestApi: UserApi,
	private val userDao: UserDao,
	private val encryptedPreference: EncryptedPreferenceRepository
) : UserRepository {

	override suspend fun authStepOne(phoneNumber: String): Resource<ConfirmPhone> {
		return mapToConfirmPhone(
			tryOnline(
				request = {
					userRestApi.postUserConfirmPhone(ConfirmPhoneRequest(phoneNumber = phoneNumber))
				}
			)
		)
	}

	override suspend fun authStepTwo(verificationCode: String, verificationId: Long): Resource<ConfirmCode> {
		return mapToConfirmCode(
			tryOnline(
				request = {
					userRestApi.postUserConfirmCode(
						ConfirmCodeRequest(
							id = verificationId,
							code = verificationCode,
							//fixme: either connect it here via encryption helper class or add new param
							userPublicKey = ""
						)
					)
				}
			)
		)
	}

	private fun mapToConfirmPhone(response: Resource<ConfirmPhoneResponse>): Resource<ConfirmPhone> {
		return Resource(
			status = response.status,
			data = response.data?.fromNetwork(),
			errorIdentification = response.errorIdentification
		)
	}

	private fun mapToConfirmCode(response: Resource<ConfirmCodeResponse>): Resource<ConfirmCode> {
		return Resource(
			status = response.status,
			data = response.data?.fromNetwork(),
			errorIdentification = response.errorIdentification
		)
	}

	override fun getUserFlow(): Flow<User?> = userDao.getUserFlow()

	override fun isUserVerified(): Boolean = encryptedPreference.isUserVerified

	override suspend fun getUserId(): String? = userDao.getUser()?.id

	override suspend fun getUser(): User? = userDao.getUser()

	override suspend fun getUserFullname(): UserProfile? {
		val user = userDao.getUser() ?: return null
		return UserProfile(
			fullname = user.getFullname(),
			photoUrl = user.photoUrl
		)
	}

	override suspend fun isUsernameAvailable(username: String): Resource<UsernameAvailable> {
		return mapToBusinessObject(
				tryOnline(
					request = { userRestApi.postUserUsernameAvailable(UsernameAvailableRequest(username = username)) }
				)
		) { usernameAvailableResponse ->
			usernameAvailableResponse?.fromNetwork()
		}
	}

	private fun <X, Y> mapToBusinessObject(response: Resource<X>, convertFunction: (X?) -> Y?): Resource<Y> {
		return Resource(
			status = response.status,
			data = convertFunction(response.data),
			errorIdentification = response.errorIdentification
		)
	}
}
