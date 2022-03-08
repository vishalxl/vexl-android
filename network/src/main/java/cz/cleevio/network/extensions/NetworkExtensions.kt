package cz.cleevio.network.extensions

import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import cz.cleevio.network.NetworkError
import cz.cleevio.network.R
import cz.cleevio.network.data.ErrorIdentification
import cz.cleevio.network.data.Resource
import cz.cleevio.network.response.BaseResponseJsonAdapter
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import retrofit2.HttpException
import retrofit2.Response
import timber.log.Timber
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

suspend fun <E, O> tryOnline(
	doOnSuccess: suspend ((O?) -> Unit) = {},
	doOnError: suspend ((Int, Int?) -> ErrorIdentification?) = { _, _ -> null },
	mapper: (E?) -> O?,
	request: suspend () -> Response<E>
): Resource<O> {
	val networkError = object : KoinComponent {
		val networkError: NetworkError by inject()
	}.networkError
	var error: ErrorIdentification? = null
	return try {
		val response = request()
		if (response.isSuccessful) {
			val mappedResponse = mapResource(
				Resource.success(response.body()),
				mapper
			)
			doOnSuccess(mappedResponse.data)
			mappedResponse
		} else {
			val subcode = response.errorBody()
				?.source()
				?.let {
					BaseResponseJsonAdapter(
						moshi = Moshi.Builder().build()
					).fromJson(it)
						?.code
						?.toIntOrNull()
			}
			val resource = Resource.error<O>(code = response.code(), subcode = subcode)
			error = doOnError(response.code(), resource.errorIdentification.subcode) ?: resource.errorIdentification
			resource
		}
	} catch (e: HttpException) {
		Timber.e(e)
		error = doOnError(e.code(), null) ?: ErrorIdentification.Unknown(e.code(), null)
		Resource.errorUnknown(code = e.code())
	} catch (e: SocketTimeoutException) {
		Timber.e(e)
		error = ErrorIdentification.ConnectionProblem
		Resource.error(error)
	} catch (e: UnknownHostException) {
		Timber.e(e)
		error = ErrorIdentification.ConnectionProblem
		Resource.error(error)
	} catch (e: IOException) {
		Timber.e(e)
		error = ErrorIdentification.Unknown()
		Resource.error(ErrorIdentification.Unknown())
	} catch (e: JsonDataException) {
		Timber.e(e)
		error = ErrorIdentification.MessageError(message = R.string.error_invalid_data_format)
		Resource.error(error)
	} finally {
		error?.let {
			networkError.sendError(error = it)
		}
	}
}

private fun <E, O> mapResource(
	originalResource: Resource<E>,
	mapper: (E?) -> O?
): Resource<O> {
	return Resource(
		status = originalResource.status,
		data = mapper(originalResource.data),
		errorIdentification = originalResource.errorIdentification
	)
}