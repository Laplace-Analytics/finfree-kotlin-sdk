package sdk.models

import sdk.base.GenericModel

enum class CollectionType {
    sector,
    industry,
    collection
}

data class AssetCollection(
    val id: CollectionId,
    val title: String,
    val stocks: List<String>,
    val type: CollectionType,
    val region: Region? = null,
    val assetClass: AssetClass? = null,
    val imageUrl: String? = null,
    val description: String? = null
) : GenericModel {

    fun copyWith(
        id: CollectionId? = null,
        title: String? = null,
        stocks: List<String>? = null,
        type: CollectionType? = null,
        region: Region? = null,
        assetClass: AssetClass? = null,
        imageUrl: String? = null,
        description: String? = null
    ): AssetCollection {
        return AssetCollection(
            id = id ?: this.id,
            title = title ?: this.title,
            stocks = stocks ?: this.stocks,
            type = type ?: this.type,
            region = region ?: this.region,
            assetClass = assetClass ?: this.assetClass,
            imageUrl = imageUrl ?: this.imageUrl,
            description = description ?: this.description
        )
    }

    fun withImageUrl(imageUrl: String): AssetCollection =
        AssetCollection(
            id = id,
            title = title,
            stocks = stocks,
            type = type,
            region = region,
            assetClass = assetClass,
            imageUrl = imageUrl,
            description = description
        )

    companion object {
        fun fromJson(json: Map<String, Any>, type: CollectionType): AssetCollection {
            return AssetCollection(
                id = json["id"] as CollectionId,
                title = json["title"] as String,
                stocks = if (json["stocks"] == null || json["stocks"] !is List<*>) {
                    emptyList()
                } else {
                    (json["stocks"] as List<*>).map { it.toString() }
                },
                type = type,
                region = if (json["region"] is String) (json["region"] as String).region() else null,
                assetClass = if (json["asset_class"] is String)(json["asset_class"] as String).assetClass() else null,
                imageUrl = json["image_url"] as String?,
                description = json["description"] as String?
            )
        }
    }

    override fun toJson(): MutableMap<String, Any> {
        val json = mutableMapOf<String, Any>()

        json["id"] = id
        json["title"] = title
        json["stocks"] = stocks.map { it }
        json["type"] = type.string()
        region?.let { json["region"] = it.string() }
        assetClass?.let { json["asset_class"] = it.string() }
        imageUrl?.let { json["image_url"] = it }
        description?.let { json["description"] = it }
        return json
    }
}


fun CollectionType.string(): String {
    return when (this) {
        CollectionType.industry -> "industry"
        CollectionType.sector -> "sector"
        CollectionType.collection -> "collection"
    }
}

typealias CollectionId = String
typealias SectorId = CollectionId
typealias IndustryId = CollectionId
