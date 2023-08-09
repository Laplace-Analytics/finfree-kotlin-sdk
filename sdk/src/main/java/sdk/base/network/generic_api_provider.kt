package sdk.base.network

import okhttp3.Response

abstract class GenericApiProvider(open val httpHandler: HTTPHandler)

typealias ResponseHandlerFunction<T> = (Response) -> ApiResponseObject<T, Any>
typealias StreamedResponseHandlerFunction<T> = (Response) -> ApiResponseObject<T,Any>

interface ApiResponseObject<T, out V> {
    val data: T?
    val responseType: V
    val message: String?
}

enum class BasicResponseTypes { Success, Error }

data class BasicResponse<T>(
    override val data: T? = null,
    override val responseType: BasicResponseTypes,
    override val message: String? = null
) : ApiResponseObject<T, BasicResponseTypes>

data class StreamData(val message: String) {
    val id: Int? get() = lines[0].split(":")[1].trim().toIntOrNull()
    val data: String get() = lines[1].split(":").drop(1).joinToString(":").trim()
    val event: String get() = lines[2].split(":")[1].trim()
    val lines: List<String> get() = message.split("\n")
}

enum class ExtendedBasicResponseTypes { SUCCESS, NOT_FOUND, SERVER_ERROR, CLIENT_ERROR, UNKNOWN_ERROR }

 data class ExtendedBasicResponse<T>(
      override val data: T?,
      override val responseType: ExtendedBasicResponseTypes,
      override val message: String?
) : ApiResponseObject<T, ExtendedBasicResponseTypes>
