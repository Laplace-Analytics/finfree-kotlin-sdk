package sdk.base.network

import jdk.internal.org.jline.utils.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import sdk.api.AccessToken
import sdk.base.logger
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

class HTTPHandler(
     var httpURL: String,
     val timeOut: Int = 10,
     val isHttps: Boolean = true
) {
    var token: AccessToken? = null
    val constantHeaders = mutableMapOf(
        "Content-Type" to "application/json; charset=utf-8"
    )
    private val client: OkHttpClient

    init {
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY

        client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(timeOut.toLong(), TimeUnit.SECONDS)
            .readTimeout(timeOut.toLong(), TimeUnit.SECONDS)
            .writeTimeout(timeOut.toLong(), TimeUnit.SECONDS)
            .build()
    }

    private fun buildHeaders(additionalHeaders: Map<String, String>?): Map<String, String> {
        val headers = constantHeaders.toMutableMap()
        additionalHeaders?.let { headers.putAll(it) }

        token?.let {
            headers["Authorization"] = "Bearer $it"
        }

        return headers
    }
    private fun Map<String, String>.toHeaders(): Headers {
        return Headers.Builder().apply {
            forEach { (key, value) -> add(key, value) }
        }.build()
    }

    private fun getURL(path: String, data: Map<String, String>?): HttpUrl {
        val urlBuilder = if (isHttps) {
            HttpUrl.Builder()
                .scheme("https")
                .host(httpURL)
        } else {
            HttpUrl.Builder()
                .scheme("http")
                .host(httpURL)
        }

        urlBuilder.addPathSegments(path)

        data?.forEach { (key, value) ->
            urlBuilder.addQueryParameter(key, value)
        }

        return urlBuilder.build()
    }

    suspend fun get(
        path: String,
        data: Map<String, String>? = null,
        additionalHeaders: Map<String, String>? = null,
        timeoutSeconds: Int? = null,
        tryAgainOnTimeout: Boolean = false
    ): Response {
        val uri = getURL(path, data)

        try {
            val request = Request.Builder()
                .url(uri)
                .headers(buildHeaders(additionalHeaders).toHeaders())
                .get()
                .build()

            return withContext(Dispatchers.IO){
                val response = client.newCall(request).execute()
                if (!response.isSuccessful && response.code == 408 && tryAgainOnTimeout) { // 408 is HTTP timeout
                    logger.error("Request to $path timed out, trying again.",response.body)
                     get(path, data, additionalHeaders, timeoutSeconds)
                }else {
                    response
                }
            }
        }catch (e: SocketTimeoutException) {
            if (tryAgainOnTimeout) {
                logger.error("Request to $path timed out, trying again.","")
                return get(path, data, additionalHeaders, timeoutSeconds)
            } else {
                logger.error("Timeout exception ${e.message}","")
                return Response.Builder()
                    .code(419)
                    .protocol(Protocol.HTTP_1_1)
                    .message("Timeout exception ${e.message}")
                    .body(ResponseBody.create(null, byteArrayOf()))
                    .build()
            }
        } catch (ex: Exception) {
            logger.error("Unknown exception with request\n$uri", ex)
            return Response.Builder()
                .code(420)
                .protocol(Protocol.HTTP_1_1)
                .message("Unknown exception")
                .body(ResponseBody.create(null, byteArrayOf()))
                .build()
        }
    }

    suspend fun post(
        path: String,
        data: Map<String, String>? = null,
        body: String? = null,
        additionalHeaders: Map<String, String>? = null,
    ): Response {
        val uri = getURL(path, data)
        try {
            val request = Request.Builder()
                .url(uri.toString())
                .headers(buildHeaders(additionalHeaders).toHeaders())
                .post(body.toString()
                    .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                )
                .build()


             return withContext(Dispatchers.IO) {
                client.newCall(request).execute() // control
            }
        } catch (ex: Exception) {
            logger.error("Unknown exception with request\n$uri\n", ex)
            return Response.Builder()
                .code(419) // Custom error code
                .protocol(Protocol.HTTP_1_1)
                .message("Unknown exception")
                .body(ResponseBody.create(null, byteArrayOf()))
                .build()
        }
    }

    suspend fun postStreamedRequest(
        path: String,
        data: Map<String, String>? = null,
        body: String? = null,
        additionalHeaders: Map<String, String>? = null
    ): Response {
        val uri = getURL(path, data)

        try {
            val request = Request.Builder()
                .url(uri.toString())
                .headers(buildHeaders(additionalHeaders).toHeaders())
                .post(body.toString()
                    .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                )
                .build()

           return withContext(Dispatchers.IO){
                client.newCall(request).execute()
           }

        } catch (ex: IOException) {
            // Handle exception, maybe log it and return an error byte array
            logger.error("Unknown exception with request\n$uri\n", ex)
            return Response.Builder()
                .code(419)
                .protocol(Protocol.HTTP_1_1)
                .message("Unknown exception")
                .body(ResponseBody.create(null, byteArrayOf()))
                .build()
        }
    }

    suspend fun getStreamedRequest(
        path: String,
        data: Map<String, String>? = null,
        additionalHeaders: Map<String, String>? = null
    ): Response {
        val uri = getURL(path, data)

        try {
            val request = Request.Builder()
                .url(uri.toString())
                .headers(buildHeaders(additionalHeaders).toHeaders())
                .get()
                .build()

            return withContext(Dispatchers.IO){
                client.newCall(request).execute()
            }

        } catch (ex: IOException) {
            // Handle exception, maybe log it and return an error byte array
            logger.error("Unknown exception with request\n$uri\n", ex)
            return Response.Builder()
                .code(419)
                .protocol(Protocol.HTTP_1_1)
                .message("Unknown exception")
                .body(ResponseBody.create(null, byteArrayOf()))
                .build()
        }
    }

    suspend fun put(
        path: String,
        data: Map<String, String>? = null,
        body: Any? = null,
        additionalHeaders: Map<String, String>? = null,
        timeoutSeconds: Int? = null,
        tryAgainOnTimeout: Boolean = false
    ): Response {
        val uri = getURL(path, data)
        try {
            val request = Request.Builder()
                .url(uri)
                .headers(buildHeaders(additionalHeaders).toHeaders())
                .put(
                    body.toString()
                        .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                )
                .build()

            return withContext(Dispatchers.IO){
                val response = client.newCall(request).execute()

                if (response.code == 408 && tryAgainOnTimeout) { // 408 is HTTP timeout
                    Log.info("Request to $path timed out, trying again.")
                     put(path, data, body, additionalHeaders, timeoutSeconds)
                }
                 response
            }
        } catch (e: TimeoutCancellationException) {
            if (tryAgainOnTimeout) {
                Log.info("Request to $path timed out, trying again.")
                return put(path, data, body, additionalHeaders, timeoutSeconds)
            } else {
                return Response.Builder()
                    .code(419)
                    .protocol(Protocol.HTTP_1_1)
                    .message("Timeout")
                    .body(ResponseBody.create(null, byteArrayOf()))
                    .build()
            }
        } catch (ex: Exception) {
            logger.error("Unknown exception with request $uri", ex)
            return Response.Builder()
                .code(419)
                .protocol(Protocol.HTTP_1_1)
                .message("Unknown exception")
                .body(ResponseBody.create(null, byteArrayOf()))
                .build()
        }
    }

    suspend fun delete(
        path: String,
        data: Map<String, String>? = null,
        body: Any? = null,
        additionalHeaders: Map<String, String>? = null,
    ): Response {
        val uri = getURL(path, data)

        try {
            val request = Request.Builder()
                .url(uri.toString())
                .headers(buildHeaders(additionalHeaders).toHeaders())
                .delete(
                    body.toString()
                        .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                )
                .build()

            val response = withContext(Dispatchers.IO) {
                client.newCall(request).execute()
            }

            return response

        } catch (ex: Exception) {
            logger.error("Unknown exception with request\n$uri\n", ex)
            // Return a dummy error response
            return Response.Builder()
                .code(419)
                .protocol(Protocol.HTTP_1_1)
                .message("Unknown exception")
                .body(ResponseBody.create(null, byteArrayOf()))
                .build()
        }
    }

    suspend fun postForm(
        path: String,
        data: Map<String, String>? = null,
        files: List<MultipartBody.Part>? = null,
        fields: Map<String, String> = mapOf(),
        additionalHeaders: Map<String, String>? = null,
    ): Response {
        val uri = getURL(path, data)

        try {
            val headers = mutableMapOf<String, String>("Content-Type" to "multipart/form-data")
            token?.let {
                headers["Authorization"] = "Bearer $it"
            }
            additionalHeaders?.let {
                headers.putAll(it)
            }

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)

            fields.forEach { (key, value) ->
                requestBody.addFormDataPart(key, value)
            }

            files?.forEach { file ->
                requestBody.addPart(file)
            }

            val request = Request.Builder()
                .url(uri.toString())
                .headers(headers.toHeaders())
                .post(requestBody.build())
                .build()

            return withContext(Dispatchers.IO) {
                client.newCall(request).execute()
            }

        } catch (ex: Exception) {
            logger.error("Unknown exception with request\n$uri\n", ex)
            // Return a dummy error response
            return Response.Builder()
                .code(419)
                .protocol(Protocol.HTTP_1_1)
                .message("Unknown exception")
                .body(ResponseBody.create(null, byteArrayOf()))
                .build()
        }
    }

    suspend fun patch(
        path: String,
        data: Map<String, String>? = null,
        body: Any? = null,
        additionalHeaders: Map<String, String>? = null,
    ): Response {
        val uri = getURL(path, data)

        try {
            val headers = buildHeaders(additionalHeaders).toHeaders()
            val request = Request.Builder()
                .url(uri.toString())
                .headers(headers)
                .patch(
                    body.toString()
                        .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                )
                .build()

            return withContext(Dispatchers.IO) {
                client.newCall(request).execute()
            }

        } catch (ex: Exception) {
            logger.error("Unknown exception with request\n$uri\n", ex)
            // Return a dummy error response
            return Response.Builder()
                .code(419)
                .protocol(Protocol.HTTP_1_1)
                .message("Unknown exception")
                .body(ResponseBody.create(null, byteArrayOf()))
                .build()
        }
    }
}