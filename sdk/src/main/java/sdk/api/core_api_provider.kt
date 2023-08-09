package sdk.api

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import sdk.base.network.*

class CoreApiProvider(
    override val httpHandler: HTTPHandler
) : GenericApiProvider(httpHandler) {

    suspend fun getPredefinedCollections(region: String): BasicResponse<List<Map<String, Any>>> {
        val path = "stock/collections"
        val response = httpHandler.get(
            path = path,
            data = mapOf("region" to region)
        )
        return ApiResponseHandler.handleResponse(
            response = response,
            onSuccess = { res ->
                BasicResponse(
                    data = Json.decodeFromString<List<Map<String, Any>>>(res.body!!.string()),
                    responseType = BasicResponseTypes.Success,
                    message = null
                )
            },
            onServerError = {
                BasicResponse(
                    data = null,
                    responseType = BasicResponseTypes.Error,
                    message = null
                )
            }
        ) as BasicResponse<List<Map<String, Any>>>
    }

    suspend fun getAllStocks(locale: String, secondsSinceEpoch: Int? = null): BasicResponse<List<Map<String, Any>>> {
        val path = "stock/all/$locale"
        val response = httpHandler.get(
            path = path,
            data = secondsSinceEpoch?.let {
                mapOf(
                    "after" to it.toString()
                )
            }
        )

        return ApiResponseHandler.handleResponse(
            response = response,
            onSuccess = { res ->
                BasicResponse(
                    data = Json.decodeFromString<List<Map<String, Any>>>(res.body!!.string()),
                    responseType = BasicResponseTypes.Success,
                    message = null
                )
            }
        ) as BasicResponse<List<Map<String, Any>>>
    }

    suspend fun getSessions(region: String? = null, assetClass: String? = null): BasicResponse<List<Any>> {
        val path = "stock/schedules"

        val data = mutableMapOf<String, String>()
        region?.let { data["region"] = it }
        assetClass?.let { data["asset_class"] = it }

        val response = httpHandler.get(
            path = path,
            data = data
        )

        return ApiResponseHandler.handleResponse(
            response = response,
            onSuccess = { res ->
                BasicResponse(
                    data = Json.decodeFromString<List<Any>>(res.body!!.string()),
                    responseType = BasicResponseTypes.Success,
                    message = null
                )
            }
        ) as BasicResponse<List<Any>>
    }

    suspend fun getCollections(locale: String, collectionType: String): BasicResponse<List<Map<String, Any>>> {
        val path = "stock/$collectionType/$locale"

        val response = httpHandler.get(path = path)

        return ApiResponseHandler.handleResponse(
            response = response,
            onSuccess = { res ->
                BasicResponse(
                    data = Json.decodeFromString<List<Map<String, Any>>>(res.body!!.string()),
                    responseType = BasicResponseTypes.Success,
                    message = null
                )
            }
        ) as BasicResponse<List<Map<String, Any>>>
    }



    suspend fun getJurisdiction(locale: String): BasicResponse<Map<String, Any>> {
        val path = "/jurisdiction/$locale"

        val response = httpHandler.get(path = path)

        return ApiResponseHandler.handleResponse(
            response = response,
            onSuccess = { res ->
                BasicResponse(
                    data = Json.decodeFromString<Map<String, Any>>(res.body!!.string()),
                    responseType = BasicResponseTypes.Success
                )
            }
        ) as BasicResponse<Map<String, Any>>
    }

    suspend fun postJurisdiction(locale: String, jurisdiction: String): BasicResponse<Unit> {
        val path = "/jurisdiction"

        val response = httpHandler.post(
            path = path,
            body = Json.encodeToString(
                mapOf(
                    "jurisdiction" to jurisdiction,
                    "locale" to locale
                )
            )
        )

        return ApiResponseHandler.handleResponse(
            response = response,
            onSuccess = {
                BasicResponse(
                    responseType = BasicResponseTypes.Success
                )
            }
        ) as BasicResponse<Unit>
    }
}
