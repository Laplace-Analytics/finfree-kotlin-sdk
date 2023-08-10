package sdk.repositories

import sdk.api.CoreApiProvider
import sdk.base.GenericRepository
import sdk.base.GenericStorage
import sdk.base.logger
import sdk.base.network.BasicResponseTypes
import sdk.models.AssetCollection
import sdk.models.CollectionType
import sdk.models.Region
import sdk.models.string

open class AssetCollectionRepo(
    override val storageHandler: GenericStorage,
    override val apiProvider: CoreApiProvider
) : GenericRepository<List<AssetCollection>, AssetCollectionRepoIdentifier, CoreApiProvider>(apiProvider = apiProvider, storageHandler = storageHandler) {

    override suspend fun fetchData(identifier: AssetCollectionRepoIdentifier?): List<AssetCollection>? {
        if (identifier == null) return null
        return try {
            val response = when (identifier.collectionType) {
                CollectionType.collection -> apiProvider.getPredefinedCollections(identifier.region.string())
                else -> apiProvider.getCollections(identifier.region.string(), identifier.collectionType.string())
            }

            if (response.responseType != BasicResponseTypes.Success || response.data == null) {
                null
            } else {
                getFromJson(mapOf("data" to response.data, "type" to identifier.collectionType.string()))
            }
        } catch (ex: Exception) {
            logger.error("error occured trying to get all stocks", ex)
            null
        }
    }

    override fun getFromJson(json: Map<String, Any>): List<AssetCollection> {
        return (json["data"] as List<Map<String, Any>>).map { data ->
            AssetCollection.fromJson(data, (json["type"] as String).collectionType())
        }
    }

    override fun getIdentifier(data: List<AssetCollection>): AssetCollectionRepoIdentifier? {
        throw NotImplementedError()
    }

    override fun getPath(identifier: AssetCollectionRepoIdentifier?): String {
        return "collections/${identifier?.region?.string()}/${identifier?.collectionType?.string()}"
    }

    override fun toJson(data: List<AssetCollection>): Map<String, Any> {
        return mapOf(
            "data" to data.map { it.toJson() },
            "type" to data.first().type.string()
        )
    }
}

data class AssetCollectionRepoIdentifier(
    val region: Region,
    val collectionType: CollectionType
)

    private fun String.collectionType(): CollectionType {
    return when (this) {
        "industry" -> CollectionType.industry
        "sector" -> CollectionType.sector
        "collection" -> CollectionType.collection
        else -> throw IllegalArgumentException("Unknown collection type: $this")
    }
}