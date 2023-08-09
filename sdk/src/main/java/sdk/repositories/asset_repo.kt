package sdk.repositories

import sdk.api.CoreApiProvider
import sdk.base.GenericRepository
import sdk.base.GenericStorage
import sdk.base.logger
import sdk.base.network.BasicResponseTypes
import sdk.models.Asset
import sdk.models.Region
import sdk.models.string

open class AssetRepo(
    override val storageHandler: GenericStorage,
    override val apiProvider: CoreApiProvider
) : GenericRepository<List<Asset>, Region, CoreApiProvider>(apiProvider = apiProvider, storageHandler = storageHandler)  {

    override suspend fun fetchData(region: Region?): List<Asset>? {
        if (region == null) return null
        return try {
            val response = apiProvider.getAllStocks(region.string())

            if (response.responseType != BasicResponseTypes.Success || response.data == null) {
                null
            } else {
                getFromJson(mapOf("assets" to response.data, "region" to region.string()))
            }
        } catch (ex: Exception) {
            logger.error("error occured trying to get all stocks", ex)
            null
        }
    }

    override fun getFromJson(json: Map<String, Any>): List<Asset> {
        return (json["assets"] as List<Map<String, Any>>).map { data ->
            Asset.fromJson(data + ("region" to json["region"]))
        }
    }

    override fun getIdentifier(data: List<Asset>): Region? {
        return if (data.isEmpty()) null else data.first().region
    }

    override fun getPath(identifier: Region?): String {
        return "assets/${identifier?.string() ?: Region.turkish.string()}"
    }

    override fun toJson(data: List<Asset>): Map<String, Any> {
        return mapOf("data" to data.map { it.toJson() })
    }
}
