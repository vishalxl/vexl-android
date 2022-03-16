package cz.cleevio.network.api

import cz.cleevio.network.request.contact.ContactRequest
import cz.cleevio.network.request.contact.ContactUserRequest
import cz.cleevio.network.request.contact.DeleteContactRequest
import cz.cleevio.network.response.contact.*
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ContactApi {

	@POST("contact/not-imported")
	suspend fun postContactNotImported(
		@Body contactRequest: ContactRequest
	): Response<ContactNotImportResponse>

	@POST("contact/import")
	suspend fun postContactImport(
		@Body contactImportRequest: ContactRequest
	): Response<ContactImportResponse>

	@GET("contact")
	suspend fun getContact(): Response<ContactResponse>

	@DELETE("contact")
	suspend fun deleteContact(
		@Body deleteContactRequest: DeleteContactRequest
	): Response<ResponseBody>

	@POST("user")
	suspend fun postUser(
		@Body contactUserRequest: ContactUserRequest
	): Response<ContactUserResponse>

	@DELETE("user/me")
	suspend fun deleteUserMe(): Response<ResponseBody>

	@GET("facebook/{facebookId}/token/{accessToken}")
	suspend fun getFacebookToken(
		@Path(value = "facebookId") facebookId: String,
		@Path(value = "accessToken") accessToken: String
	): Response<ContactFacebookResponse>

	@GET("facebook/{facebookId}/token/{accessToken}/new")
	suspend fun getFacebookTokenNew(
		@Path(value = "facebookId") facebookId: String,
		@Path(value = "accessToken") accessToken: String
	): Response<ContactFacebookResponse>
}