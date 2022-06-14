package cz.cleevio.network.api

import cz.cleevio.network.request.chat.*
import cz.cleevio.network.response.chat.ChallengeCreatedResponse
import cz.cleevio.network.response.chat.InboxResponse
import cz.cleevio.network.response.chat.MessagesResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ChatApi {

	//update inbox with new firebase token
	@PUT("inboxes")
	suspend fun putInboxes(
		@Body inboxRequest: UpdateInboxRequest
	): Response<InboxResponse>

	//create new inbox
	@POST("inboxes")
	suspend fun postInboxes(
		@Body inboxRequest: CreateInboxRequest
	): Response<ResponseBody>

	//todo: fixme
	//retrieve messages from inbox
	@PUT("inboxes/messages")
	suspend fun putInboxesMessages(
		@Body messageRequest: MessageRequest
		//todo: should be list inside some other object
	): Response<MessagesResponse>

	//send a message to inbox
	@POST("inboxes/messages")
	suspend fun postInboxesMessages(
		@Body sendMessageRequest: SendMessageRequest
	): Response<ResponseBody>

	//block/unblock sender
	@PUT("inboxes/block")
	suspend fun putInboxesBlock(
		@Body blockRequest: BlockInboxRequest
	): Response<ResponseBody>

	//approve communication request
	@POST("inboxes/approval/request")
	suspend fun postInboxesApprovalRequest(
		@Body approvalRequest: ApprovalRequest
	): Response<ResponseBody>

	//approve communication request
	@POST("inboxes/approval/confirm")
	suspend fun postInboxesApprovalConfirm(
		@Body approvalRequest: ApprovalConfirmRequest
	): Response<ResponseBody>

	//delete whole inbox
	@DELETE("inboxes/{publicKey}")
	suspend fun deleteInboxes(
		@Path(value = "publicKey") publicKey: String
	): Response<ResponseBody>

	//delete pulled messages
	@DELETE("inboxes/{publicKey}/messages")
	suspend fun deleteInboxesMessages(
		@Path(value = "publicKey") publicKey: String
	): Response<ResponseBody>

	//create a new challenge
	@POST("challenges")
	suspend fun postChallenge(
		@Body challengeRequest: CreateChallengeRequest
	): Response<ChallengeCreatedResponse>
}