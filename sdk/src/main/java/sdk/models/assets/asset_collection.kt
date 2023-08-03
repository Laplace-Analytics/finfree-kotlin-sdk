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
    val type: CollectionType
) : GenericModel {

    fun copyWith(
        id: CollectionId? = null,
        title: String? = null,
        stocks: List<String>? = null,
        type: CollectionType? = null
    ): AssetCollection {
        return AssetCollection(
            id = id ?: this.id,
            title = title ?: this.title,
            stocks = stocks ?: this.stocks,
            type = type ?: this.type
        )
    }

    companion object {
        fun fromJson(json: Map<String, Any>, type: CollectionType): AssetCollection {
            val stocks = (json["stocks"] as? List<Any>)?.mapNotNull { it as? String } ?: emptyList()
            return AssetCollection(
                id = json["id"] as CollectionId,
                title = json["title"] as String,
                stocks = stocks,
                type = type
            )
        }
    }

    override fun toJson(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "title" to title,
            "stocks" to stocks
        )
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
