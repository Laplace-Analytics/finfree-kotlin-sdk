package sdk.api

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import sdk.base.network.*
import sdk.models.data.assets.AssetClass
import sdk.models.CollectionType
import sdk.models.data.assets.Region
import sdk.models.data.assets.localeString
import sdk.models.data.assets.string
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

                val responseBodyStr = res.body?.string() ?: ""
                val type = object : TypeToken<List<Map<String, Any>>>() {}.type

                val result: List<Map<String, Any>> = Gson().fromJson(responseBodyStr,type)
                BasicResponse(
                    data = result,
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

    suspend fun getAllStocks(region: Region, secondsSinceEpoch: Long? = null): BasicResponse<List<Map<String, Any>>> {
        val path = "stock/all/${region.string()}"
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

                val responseBodyStr = res.body?.string() ?: ""
                val type = object : TypeToken<List<Map<String, Any>>>() {}.type

                val result: List<Map<String, Any>> = Gson().fromJson(responseBodyStr,type)
                BasicResponse(
                    data = result,
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

                val responseBodyStr = res.body?.string() ?: ""
                val type = object : TypeToken<List<Any>>() {}.type

                val result: List<Any> = Gson().fromJson(responseBodyStr,type)
                BasicResponse(
                    data = result,
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

                val responseBodyStr = res.body?.string() ?: ""
                val type = object : TypeToken<List<Map<String, Any>>>() {}.type

                val result: List<Map<String, Any>> = Gson().fromJson(responseBodyStr,type)
                BasicResponse(
                    data = result,
                    responseType = BasicResponseTypes.Success,
                    message = null
                )
            }
        ) as BasicResponse<List<Map<String, Any>>>
    }



    suspend fun getJurisdictionConfig(): BasicResponse<Map<String, Any>> {
        val path = "/jurisdiction/config"

        val response = httpHandler.get(path = path)

        return ApiResponseHandler.handleResponse(
            response = response,
            onSuccess = { res ->

                val responseBodyStr = res.body?.string() ?: ""
                val type = object : TypeToken<Map<String, Any>>() {}.type

                val result: Map<String, Any> = Gson().fromJson(responseBodyStr,type)
                BasicResponse(
                    data = result,
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

                val responseBodyStr = res.body?.string() ?: ""
                val type = object : TypeToken<Map<String, Any>>() {}.type

                val result: Map<String, Any> = Gson().fromJson(responseBodyStr,type)
                BasicResponse(
                    data = result,
                    responseType = BasicResponseTypes.Success,
                    message = null
                )
            },
        ) as BasicResponse<Map<String, Any>>
    }
    suspend fun getMarketStatus(): BasicResponse<Boolean> {
        val path = "marketstatus"

        val response = httpHandler.get(
            path = path,
            data = null,
            tryAgainOnTimeout = true
        )

        return ApiResponseHandler.handleResponse(
            response = response,
            onSuccess = { res ->
                BasicResponse(
                    data = response.body.toString() == "OPEN",
                    responseType = BasicResponseTypes.Success,
                    message = null
                )
            }
        ) as BasicResponse<Boolean>
    }
}
