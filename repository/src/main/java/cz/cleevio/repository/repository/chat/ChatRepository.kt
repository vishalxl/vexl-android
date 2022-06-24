package cz.cleevio.repository.repository.chat

import com.cleevio.vexl.cryptography.model.KeyPair
import cz.cleevio.network.data.Resource
import cz.cleevio.repository.model.chat.ChatListUser
import cz.cleevio.repository.model.chat.ChatMessage
import cz.cleevio.repository.model.chat.CommunicationRequest
import kotlinx.coroutines.flow.SharedFlow

interface ChatRepository {

	suspend fun saveFirebasePushToken(token: String)

	//-----------------------------
	suspend fun createInbox(publicKey: String): Resource<Unit>

	suspend fun syncMessages(keyPair: KeyPair): Resource<Unit>

	suspend fun syncAllMessages(): Resource<Unit>

	suspend fun deleteMessagesFromBE(publicKey: String): Resource<Unit>

	fun getMessages(inboxPublicKey: String, firstKey: String, secondKey: String): SharedFlow<List<ChatMessage>>

	suspend fun sendMessage(
		senderPublicKey: String, receiverPublicKey: String, message: ChatMessage,
		messageType: String
	): Resource<Unit>

	suspend fun changeUserBlock(senderKeyPair: KeyPair, publicKeyToBlock: String, block: Boolean): Resource<Unit>

	suspend fun askForCommunicationApproval(publicKey: String, message: ChatMessage): Resource<Unit>

	suspend fun confirmCommunicationRequest(
		offerId: String,
		publicKeyToConfirm: String,
		message: ChatMessage,
		originalRequestMessage: ChatMessage,
		approve: Boolean
	): Resource<Unit>

	suspend fun deleteInbox(publicKey: String): Resource<Unit>

	suspend fun loadCommunicationRequests(): List<CommunicationRequest>

	suspend fun loadChatUsers(): List<ChatListUser>

	suspend fun deleteMessage(communicationRequest: CommunicationRequest)
	suspend fun getMyInboxKeys(): List<String>
}