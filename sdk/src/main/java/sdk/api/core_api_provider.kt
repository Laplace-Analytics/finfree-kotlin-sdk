package sdk.api

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import sdk.base.network.*
import sdk.models.AssetClass
import sdk.models.CollectionId
import sdk.models.CollectionType
import sdk.models.Region
import sdk.models.localeString
import sdk.models.string

class CoreApiProvider(
    override val httpHandler: HTTPHandler
) : GenericApiProvider(httpHandler) {

    suspend fun getPredefinedCollections(region: Region): BasicResponse<List<Map<String, Any>>> {
        val path = "stock/collections"
        val response = httpHandler.get(
            path = path,
            data = mapOf("region" to region.string())
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

    suspend fun getAllStocks(region: Region, secondsSinceEpoch: Int? = null): BasicResponse<List<Map<String, Any>>> {
        val path = "stock/all/${region.localeString()}"
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

    suspend fun getSessions(region: Region? = null, assetClass: AssetClass? = null): BasicResponse<List<Any>> {
        val path = "stock/schedules"

        val data = mutableMapOf<String, String>()
        region?.let { data["region"] = it.string() }
        assetClass?.let { data["asset_class"] = it.string() }

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

    suspend fun getCollections(region: Region, collectionType: CollectionType): BasicResponse<List<Map<String, Any>>> {
        val path = "stock/$collectionType/${region.string()}"

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



    suspend fun getJurisdiction(region: Region): BasicResponse<Map<String, Any>> {
        val path = "/jurisdiction/${region.string()}"

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

    suspend fun postJurisdiction(locale: Region, jurisdiction: Region): BasicResponse<Unit> {
        val path = "/jurisdiction"

        val response = httpHandler.post(
            path = path,
            body = Json.encodeToString(
                mapOf(
                    "jurisdiction" to jurisdiction.string(),
                    "locale" to locale.localeString()
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

    suspend fun getCollectionDetail(collectionId: String,type: CollectionType): BasicResponse<Map<String, Any>> {
        val path = "stock/collections/detail"
        val response = httpHandler.get(
            path = path,
            data = mapOf(
                "type" to type.string(),
                "id" to collectionId
            )
        )
        return ApiResponseHandler.handleResponse(
            response = response,
            onSuccess = { res ->
                BasicResponse(
                    data = Json.decodeFromString<Map<String, Any>>(res.body!!.string()),
                    responseType = BasicResponseTypes.Success,
                    message = null
                )
            },
        ) as BasicResponse<Map<String, Any>>
    }
}
