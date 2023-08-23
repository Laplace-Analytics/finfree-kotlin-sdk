package sdk.base.network

import kotlinx.serialization.json.Json
import okhttp3.Response
import sdk.base.logger


class ApiResponseHandler {
    companion object {
        fun <T : Any> handleResponse(
            response: Response,
            onSuccess: ResponseHandlerFunction<T>? = null,
            onOk: ResponseHandlerFunction<T>? = null,
            onCreated: ResponseHandlerFunction<T>? = null,
            onAccepted: ResponseHandlerFunction<T>? = null,
            onError: ResponseHandlerFunction<T>? = null,
            onClientError: ResponseHandlerFunction<T>? = null,
            onBadRequest: ResponseHandlerFunction<T>? = null,
            onUnauthorized: ResponseHandlerFunction<T>? = null,
            onForbidden: ResponseHandlerFunction<T>? = null,
            onNotFound: ResponseHandlerFunction<T>? = null,
            onImATeapot: ResponseHandlerFunction<T>? = null,
            onMethodNotAllowed: ResponseHandlerFunction<T>? = null,
            onConflict: ResponseHandlerFunction<T>? = null,
            onFailedDependency: ResponseHandlerFunction<T>? = null,
            onConnectionClosedWithoutResponse: ResponseHandlerFunction<T>? = null,
            onServerError: ResponseHandlerFunction<T>? = null,
            onTooManyRequest: ResponseHandlerFunction<T>? = null,
            onNoContent: ResponseHandlerFunction<T>? = null
        ): ApiResponseObject<T, Any> {
            return when {
                response.code >= 500 -> {
                    handleCase(response, onServerError,  onError ?:  defaultErrorResponse())
                }
                response.code >= 300 -> {
                    val specialStatusHandler = when (response.code) {
                        400 -> onBadRequest
                        401 -> onUnauthorized
                        403 -> onForbidden
                        404 -> onNotFound
                        405 -> onMethodNotAllowed
                        409 -> onConflict
                        424 -> onFailedDependency
                        444 -> onConnectionClosedWithoutResponse
                        429 -> onTooManyRequest
                        419 -> onImATeapot
                        else -> null
                    }
                    handleCase(response, specialStatusHandler, onClientError ?: (onError ?: defaultErrorResponse()))
                }
                else -> {
                    val specialStatusHandler = when (response.code) {
                        200 -> onOk
                        201 -> onCreated
                        202 -> onAccepted
                        204 -> onNoContent
                        else -> null
                    }

                    try {
                       handleCase(response, specialStatusHandler, onSuccess ?: defaultSuccessResponse())
                    } catch (ex:Exception) {
                        logger.error(
                            "error occurred while trying to parse response",
                            ex,
                            )
                        onError?.invoke(response) ?: BasicResponse<T>(
                            data = null,
                            responseType = BasicResponseTypes.Error,
                            message = "Error occurred while handling successful response"
                        )
                    }
                }
            }
        }

        fun <T : Any> handleStreamedResponse(
            response: Response,
            onSuccess: ResponseHandlerFunction<T>? = null,
            onOk: ResponseHandlerFunction<T>? = null,
            onCreated: ResponseHandlerFunction<T>? = null,
            onAccepted: ResponseHandlerFunction<T>? = null,
            onError: ResponseHandlerFunction<T>? = null,
            onClientError: ResponseHandlerFunction<T>? = null,
            onBadRequest: ResponseHandlerFunction<T>? = null,
            onUnauthorized: ResponseHandlerFunction<T>? = null,
            onForbidden: ResponseHandlerFunction<T>? = null,
            onNotFound: ResponseHandlerFunction<T>? = null,
            onImATeapot: ResponseHandlerFunction<T>? = null,
            onMethodNotAllowed: ResponseHandlerFunction<T>? = null,
            onConflict: ResponseHandlerFunction<T>? = null,
            onFailedDependency: ResponseHandlerFunction<T>? = null,
            onConnectionClosedWithoutResponse: ResponseHandlerFunction<T>? = null,
            onServerError: ResponseHandlerFunction<T>? = null,
            onTooManyRequest: ResponseHandlerFunction<T>? = null,
            onNoContent: ResponseHandlerFunction<T>? = null
        ): ApiResponseObject<T, Any> {
            return when {
                response.code >= 500 -> {
                    handleStreamedCase(response, onServerError,  onError ?:  defaultStreamedErrorResponse())
                }
                response.code >= 300 -> {
                    val specialStatusHandler = when (response.code) {
                        400 -> onBadRequest
                        401 -> onUnauthorized
                        403 -> onForbidden
                        404 -> onNotFound
                        405 -> onMethodNotAllowed
                        409 -> onConflict
                        424 -> onFailedDependency
                        444 -> onConnectionClosedWithoutResponse
                        429 -> onTooManyRequest
                        419 -> onImATeapot
                        else -> null
                    }
                    handleStreamedCase(response, specialStatusHandler, onClientError ?: (onError ?: defaultStreamedErrorResponse()))
                }
                else -> {
                    val specialStatusHandler = when (response.code) {
                        200 -> onOk
                        201 -> onCreated
                        202 -> onAccepted
                        204 -> onNoContent
                        else -> null
                    }

                    try {
                        handleStreamedCase(response, specialStatusHandler, onSuccess ?: defaultStreamedSuccessResponse())
                    } catch (ex:Exception) {
                        logger.error(
                            "error occurred while trying to parse response",
                            ex,
                        )
                        onError?.invoke(response) ?: BasicResponse<T>(
                            data = null,
                            responseType = BasicResponseTypes.Error,
                            message = "Error occurred while handling successful response"
                        )
                    }
                }
            }
        }


        private fun <T> handleCase(
            response: Response,
            specialStatus: ResponseHandlerFunction<T>? = null,
            generalStatus: ResponseHandlerFunction<T>
        ): ApiResponseObject<T, Any> {
            return if (specialStatus != null) {
                specialStatus(response)
            }else{
                generalStatus(response)
            }
        }

        private fun <T> handleStreamedCase(
            response: Response,
            specialStatus: StreamedResponseHandlerFunction<T>? = null,
            generalStatus: StreamedResponseHandlerFunction<T>
        ): ApiResponseObject<T, Any> {
            return if (specialStatus != null) {
                specialStatus(response)
            }else{
                generalStatus(response)
            }
        }



        fun decodeJson(source: String): Any {
            return try {
                Json.parseToJsonElement(source)
            } catch (e: Exception) {
                source.trim()
            }
        }

        private fun <T : Any> defaultErrorResponse(): ResponseHandlerFunction<T> = { response ->
            var responseBody: String? = null
            responseBody = response.body?.string()
            val statusCode = response.code
            logger.error("Error with API call, received status code $statusCode.\n$responseBody\n$response")

            BasicResponse(data = null, responseType = BasicResponseTypes.Error, message = responseBody)
        }

        private fun <T : Any> defaultStreamedErrorResponse(): StreamedResponseHandlerFunction<T> = { response ->
            val statusCode = response.code
            logger.error("Error with API call, received status code $statusCode.\n\n$response")

            BasicResponse(data = null, responseType = BasicResponseTypes.Error, message = null)
        }

        private fun <T: Any> defaultSuccessResponse(): ResponseHandlerFunction<T> = { response ->
            var responseBody: String? = null

            responseBody = response.body?.string()

            BasicResponse(data = null, responseType = BasicResponseTypes.Success, message = responseBody)
        }

        private fun <T: Any> defaultStreamedSuccessResponse(): ResponseHandlerFunction<T> = {
            BasicResponse(data = null, responseType = BasicResponseTypes.Success, message = null)
        }

    }
}