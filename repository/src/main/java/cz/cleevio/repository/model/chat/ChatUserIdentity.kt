package cz.cleevio.repository.model.chat

import cz.cleevio.cache.entity.ChatUserIdentityEntity

data class ChatUserIdentity(
	val id: Long,
	val contactPublicKey: String,
	val inboxKey: String,
	val name: String? = null,
	val anonymousUsername: String? = null,
	val avatar: String? = null,
	val anonymousAvatarImageIndex: Int? = null,
	val deAnonymized: Boolean
)

fun ChatUserIdentityEntity.fromCache(): ChatUserIdentity {
	return ChatUserIdentity(
		id = this.id,
		contactPublicKey = this.contactPublicKey,
		inboxKey = this.inboxKey,
		name = this.name,
		anonymousUsername = this.anonymousUsername,
		avatar = this.avatar,
		anonymousAvatarImageIndex = this.anonymousAvatarImageIndex,
		deAnonymized = this.deAnonymized
	)
}