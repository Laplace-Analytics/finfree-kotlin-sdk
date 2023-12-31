package sdk.repositories

import sdk.api.CoreApiProvider
import sdk.base.GenericRepository
import sdk.base.GenericStorage
import sdk.base.logger
import sdk.base.network.BasicResponseTypes
import sdk.models.AssetCollection
import sdk.models.CollectionId
import sdk.models.CollectionType

open class AssetCollectionDetailRepo(
    override val storageHandler: GenericStorage,
    override val apiProvider: CoreApiProvider
    ) : GenericRepository<AssetCollection, AssetCollectionDetailRepoIdentifier, CoreApiProvider>(storageHandler, apiProvider) {


    override suspend fun fetchData(identifier: AssetCollectionDetailRepoIdentifier?): AssetCollection? {
        identifier ?: return null

        val id = identifier.id
        val type = identifier.collectionType

        return try {
            val response = apiProvider.getCollectionDetail(id, type)

            if (response.responseType != BasicResponseTypes.Success || response.data == null) {
                return null
            }

            getFromJson(mapOf("data" to response.data, "type" to type))
        } catch (e: Exception) {
            logger.error("AssetCollectionRepo.getData exception", e)
            null
        }
    }

    override fun getFromJson(json: Map<String, Any>): AssetCollection {
        val typeString = json["type"] as CollectionType
        return AssetCollection.fromJson((json["data"] as Map<String, Any>)["collection"] as Map<String, Any>, typeString)
    }

    override fun getIdentifier(data: AssetCollection): AssetCollectionDetailRepoIdentifier? {
        return AssetCollectionDetailRepoIdentifier(data.id, data.type)
    }

    override fun getPath(identifier: AssetCollectionDetailRepoIdentifier?): String {
        return "asset_collections/${identifier?.id}"
    }

    override fun toJson(data: AssetCollection): Map<String, Any> {
        return data.toJson()
    }
}

data class AssetCollectionDetailRepoIdentifier(
    val id: CollectionId,
    val collectionType: CollectionType
)

