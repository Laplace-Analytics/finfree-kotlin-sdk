package sdk.trade.models.portfolio

import sdk.base.GenericModel

data class DriveWealthAccountDocument(
    val displayName: String,
    val fileKey: String,
    val url: String
) : GenericModel {
    override fun toJson(): Map<String, Any> {
        return mapOf(
            "displayName" to displayName,
            "fileKey" to fileKey,
            "url" to url
        )
    }

    companion object {
        fun fromJson(json: Map<String, Any>): DriveWealthAccountDocument {
            return DriveWealthAccountDocument(
                displayName = json["displayName"].toString(),
                fileKey = json["fileKey"].toString(),
                url = json["url"].toString()
            )
        }
    }
}
