package cz.cleevio.repository.repository.user

import cz.cleevio.cache.dao.UserDao
import cz.cleevio.cache.entity.UserEntity
import cz.cleevio.cache.preferences.EncryptedPreferenceRepository
import cz.cleevio.network.api.UserApi
import cz.cleevio.network.data.Resource
import cz.cleevio.network.extensions.tryOnline
import cz.cleevio.network.request.user.*
import cz.cleevio.repository.model.UserProfile
import cz.cleevio.repository.model.user.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserRepositoryImpl constructor(
	private val userRestApi: UserApi,
	private val userDao: UserDao,
	private val encryptedPreference: EncryptedPreferenceRepository
) : UserRepository {

	override suspend fun authStepOne(phoneNumber: String): Resource<ConfirmPhone> {
		return tryOnline(
			mapper = {
				it?.fromNetwork()
			},
			request = { userRestApi.postUserConfirmPhone(ConfirmPhoneRequest(phoneNumber = phoneNumber)) }
		)
	}

	override suspend fun authStepTwo(verificationCode: String, verificationId: Long): Resource<ConfirmCode> {
		return tryOnline(
			mapper = {
				it?.fromNetwork()
			},
			request = {
				userRestApi.postUserConfirmCode(
					ConfirmCodeRequest(
						id = verificationId,
						code = verificationCode,
						userPublicKey = encryptedPreference.userPublicKey
					)
				)
			}
		)
	}

	override suspend fun authStepThree(signature: String): Resource<Signature> {
		return tryOnline(
			mapper = {
				it?.fromNetwork()
			},
			request = {
				userRestApi.postUserConfirmChallenge(
					ConfirmChallengeRequest(
						userPublicKey = encryptedPreference.userPublicKey,
						signature = signature
					)
				)
			}
		)
	}

	override suspend fun registerFacebookUser(facebookId: String): Resource<Signature> = tryOnline(
		request = { userRestApi.getUserSignatureFacebook(facebookId = facebookId) },
		mapper = { it?.fromNetwork() },
		doOnSuccess = {
			it?.let { data ->
				encryptedPreference.facebookHash = data.hash
				encryptedPreference.facebookSignature = data.signature
			}
		}
	)

	override fun getUserFlow(): Flow<User?> = userDao.getUserFlow().map { it?.fromDao() }

	override fun isUserVerified(): Boolean = encryptedPreference.isUserVerified

	override suspend fun createUser(user: User) {
		userDao.insert(
			UserEntity(
				extId = user.extId,
				username = user.username,
				avatar = user.avatar,
				publicKey = user.publicKey
			)
		)
	}

	override suspend fun getUserId(): Long? = userDao.getUser()?.id

	override suspend fun getUser(): User? = userDao.getUser()?.fromDao()

	override suspend fun getUserFullname(): UserProfile? {
		val user = userDao.getUser() ?: return null
		return UserProfile(
			fullname = user.username,
			photoUrl = user.avatar
		)
	}

	override suspend fun registerUser(username: String, avatar: String, avatarImageExtension: String): Resource<User> {
		return tryOnline(
			doOnSuccess = {
				it?.let {
					createUser(it)
				}
			},
			mapper = {
				it?.fromNetwork()
			},
			request = {
				userRestApi.postUser(
					UserRequest(
						username = username,
						avatar = UserAvatar(
							data = avatar,
							extension = avatarImageExtension
						)
					)
				)
			}
		)
	}

	override suspend fun isUsernameAvailable(username: String): Resource<UsernameAvailable> {
		return tryOnline(
			mapper = {
				it?.fromNetwork(username)
			},
			request = { userRestApi.postUserUsernameAvailable(UsernameAvailableRequest(username = username)) }
		)
	}

	override suspend fun deleteMe(): Resource<Unit> = tryOnline(
		request = { userRestApi.deleteUserMe() },
		mapper = { }
	)
}
