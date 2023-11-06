package sdk.repositories

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import sdk.api.CoreApiProvider
import sdk.base.GenericRepository
import sdk.base.GenericStorage
import sdk.base.logger
import sdk.base.network.BasicResponseTypes
import sdk.models.data.assets.Asset
import sdk.models.data.assets.Region
import sdk.models.core.sessions.DateTime
import sdk.models.core.sessions.DateTime.Companion.toEpochMilliSecond
import sdk.models.data.assets.string
import java.time.LocalDateTime

open class AssetRepo(
    override val storageHandler: GenericStorage,
    override val apiProvider: CoreApiProvider
) : GenericRepository<List<Asset>, AssetsRepoIdentifier, CoreApiProvider>(apiProvider = apiProvider, storageHandler = storageHandler)  {


    override suspend fun getData(identifier: AssetsRepoIdentifier?): List<Asset>? {
        if (identifier?.region == null) {
            return null
        }
        val cachedData: List<Asset>? = readData(identifier)
        if (!cachedData.isNullOrEmpty()) {
            val lastUpdatedDate: LocalDateTime = storageHandler.getLastModified(getPath(identifier))
                ?: return null
            val lastUpdatedDateSecondsSinceEpoch = lastUpdatedDate.toEpochMilliSecond() / 1000

            return checkCachedAssetsAndUpdateIfNecessary(
                identifier,
                lastUpdatedDateSecondsSinceEpoch,
                cachedData
            )
        }

        val fetchedAssets: List<Asset>? = fetchData(identifier)
        if (fetchedAssets != null) {
            try {
                saveData(fetchedAssets)
            } catch (ex: NotImplementedError) {
                // do nothing
            } catch (ex: Exception) {
                logger.error("Error saving data to local storage", ex)
            }
        }
        return fetchedAssets
    }

    private suspend fun checkCachedAssetsAndUpdateIfNecessary(
        identifier: AssetsRepoIdentifier?,
        lastUpdatedDateSecondsSinceEpoch: Long,
        cachedAssetList: List<Asset>
    ): List<Asset> {
        try {
            if (identifier == null){
                throw Exception("Identifier is null")
            }
            val changedAssets: List<Asset>? = fetchData(
                AssetsRepoIdentifier(
                    region = identifier.region,
                    secondsSinceEpoch = lastUpdatedDateSecondsSinceEpoch
                )
            )

            if (changedAssets.isNullOrEmpty()) {
                return cachedAssetList
            }

            val assetsMap = getAssetMapFromCachedAssets(cachedAssetList).toMutableMap()

            for (asset in changedAssets) {
                assetsMap[asset.id] = asset
            }

            saveData(assetsMap.values.toList())
            return getFromJson(
                mapOf(
                    "assets" to assetsMap.values.toList(),
                    "region" to identifier.region!!
                )
            )
        } catch (e: Exception) {
            logger.error("error occured while trying to check if cached assets updated $e")
            return emptyList()
        }
    }

    private fun getAssetMapFromCachedAssets(cachedList: List<Asset>): Map<String, Asset> {
        return cachedList.associateBy { it.id }
    }

    override suspend fun readData(identifier: AssetsRepoIdentifier?): List<Asset>? {
        try {
            val path = getPath(identifier)
            val lastUpdated = storageHandler.getLastModified(path)

            if (lastUpdated == null || lastUpdated.isBefore(DateTime.now().minus(cacheDuration))) {
                return null
            }

            val data = storageHandler.read(path) ?: return null

            val listType = object : TypeToken<List<Map<String, Any>>>() {}.type

            val dataMap = Gson().fromJson<Map<String, List<Map<String, Any>>>>(data, listType)
            return getFromJson(
                mapOf(
                    "assets" to dataMap["data"] as List<Map<String, Any>>,
                    "region" to identifier?.region?.string() as String
                )
            )
        } catch (ex: NotImplementedError) {
            // do nothing
        } catch (ex: Exception) {
            logger.error("Error reading data from local storage", ex)
        }
        return null
    }

    override suspend fun fetchData(identifier:AssetsRepoIdentifier?): List<Asset>? {
        if (identifier?.region == null) return null
        return try {
            val response = apiProvider.getAllStocks(
                identifier.region,
                secondsSinceEpoch = identifier.secondsSinceEpoch,
                )

            if (response.responseType != BasicResponseTypes.Success || response.data == null) {
                return null
            } else {
                getFromJson(mapOf("assets" to response.data, "region" to identifier.region.string()))
            }
        } catch (ex: Exception) {
            logger.error("error occured trying to get all stocks", ex)
            return null
        }
    }

    override fun getFromJson(json: Map<String, Any?>): List<Asset> {
        return (json["assets"] as List<Map<String, Any>>).map { data ->
            Asset.fromJson(data + ("region" to json["region"]))
        }
    }

    override fun getIdentifier(data: List<Asset>): AssetsRepoIdentifier? {
        return if (data.isEmpty()) null else  AssetsRepoIdentifier(
            region =  data.first().region,
        )
    }

    override fun getPath(identifier: AssetsRepoIdentifier?): String {
        return "assets/${identifier?.region?.string() ?: Region.Turkish.string()}"
    }

    override fun toJson(data: List<Asset>): Map<String, Any> {
        return mapOf("data" to data.map { it.toJson() })
    }
}

data class AssetsRepoIdentifier(
    val region: Region? = null,
    val secondsSinceEpoch: Long? = null
)

