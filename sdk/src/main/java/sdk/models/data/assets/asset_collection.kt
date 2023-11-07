package sdk.models.data.assets

import sdk.base.GenericModel

enum class CollectionType {
    Sector,
    Industry,
    Collection
}

data class AssetCollection(
    val id: CollectionId,
    val title: String,
    val stocks: List<String>? = null,
    val type: CollectionType,
    val region: Region,
    val assetClass: AssetClass,
    val imageUrl: CollectionImageUrl? = null,
    val description: String? = null
) : GenericModel {
    val hasStocks: Boolean
        get() = !stocks.isNullOrEmpty()

    val hasImage: Boolean
        get() = imageUrl?.avatarUrl != null


    fun copyWith(
        id: CollectionId? = null,
        title: String? = null,
        stocks: List<String>? = null,
        type: CollectionType? = null,
        region: Region? = null,
        assetClass: AssetClass? = null,
        imageUrl: CollectionImageUrl? = null,
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

    fun withImageUrl(imageUrl: CollectionImageUrl): AssetCollection =
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
        fun fromShortJson(
            json: Map<String, Any>,
            region: Region,
            assetClass: AssetClass
        ): AssetCollection {
            val value = json["value"] as Map<String, Any>

            return AssetCollection(
                id = json["id"] as String,
                title = value["title"] as String,
                imageUrl = CollectionImageUrl.fromJson(value),
                type = (json["type"] as String).collectionType(),
                region = region,
                assetClass = assetClass
            )
        }
        fun fromJson(json: Map<String, Any>, type: CollectionType): AssetCollection {
            val region = if (json["region"] is String) (json["region"] as String).region() else null
            var assetClass = if (json["asset_class"] is String) (json["asset_class"] as String).assetClass() else null

            if (region == null) {
                throw Exception("Invalid region: $region")
            }

            if (assetClass == null) {
                if (type == CollectionType.Sector || type == CollectionType.Industry) {
                    assetClass = AssetClass.Equity
                } else {
                    throw Exception("Invalid assetClass or type: $type $assetClass")
                }
            }


            val stocks = if (json["stocks"] == null || json["stocks"] !is List<*>) null else
                (json["stocks"] as List<String>).map { it }

            return AssetCollection(
                id = json["id"] as String,
                title = json["title"] as String,
                stocks = stocks,
                type = type,
                region = region,
                assetClass = assetClass,
                imageUrl = CollectionImageUrl.fromJson(json),
                description = if (json["description"] == null)  null else json["description"] as String
            )
        }
    }
    override fun toJson(): Map<String, Any?> {
        val jsonMap = mutableMapOf<String, Any?>()

        val collectionMap = mutableMapOf(
            "id" to id,
            "title" to title,
            "stocks" to stocks,
            "type" to type.toString(),
            "region" to region.toString(),
            "asset_class" to assetClass.toString()
        )

        if (imageUrl != null) {
            val imageUrlMap = imageUrl.toJson()
            collectionMap.putAll(imageUrlMap)
        }

        collectionMap["description"] = description
        jsonMap["data"] = mapOf("collection" to collectionMap)
        jsonMap["type"] = type.toString()

        return jsonMap
    }
}

class CollectionImageUrl(
    private val imageUrl: String?,
    private val _downScaledImageUrl: String?,
    private val _avatarUrl: String?
) {
    val avatarUrl: String?
        get() = _avatarUrl ?: _downScaledImageUrl ?: imageUrl

    val downScaledImageUrl: String?
        get() = _downScaledImageUrl ?: imageUrl

    fun toJson(): Map<String, Any?> {
        return mapOf(
            "image_url" to imageUrl,
            "downscaled_image_url" to _downScaledImageUrl,
            "avatar_url" to _avatarUrl
        )
    }

    companion object {
        fun fromJson(json: Map<String, Any?>?): CollectionImageUrl {
            json ?: throw Exception("CollectionImageUrl.fromJson: json is null")

            return CollectionImageUrl(
                json["image_url"]?.takeIf { it is String && it.isNotEmpty() } as? String,
                json["downscaled_image_url"]?.takeIf { it is String && it.isNotEmpty() } as? String,
                json["avatar_url"]?.takeIf { it is String && it.isNotEmpty() } as? String
            )
        }
    }
}



fun CollectionType.string(): String {
    return when (this) {
        CollectionType.Industry -> "industry"
        CollectionType.Sector -> "sector"
        CollectionType.Collection -> "collection"
    }
}

fun String.collectionType(): CollectionType {
    return when (this) {
        "industry" -> CollectionType.Industry
        "sector" -> CollectionType.Sector
        "collection" -> CollectionType.Collection
        else -> throw IllegalArgumentException("Unknown collection type: $this")
    }
}


typealias CollectionId = String
typealias SectorId = CollectionId
typealias IndustryId = CollectionId
