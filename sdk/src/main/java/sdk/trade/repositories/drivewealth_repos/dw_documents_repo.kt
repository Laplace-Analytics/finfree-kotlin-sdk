package sdk.trade.repositories.drivewealth_repos

import sdk.base.GenericRepository
import sdk.base.GenericStorage
import sdk.base.logger
import sdk.base.network.BasicResponseTypes
import sdk.trade.DriveWealthAccountApiProvider
import sdk.trade.DriveWealthDocumentTypes
import sdk.trade.models.portfolio.DriveWealthAccountDocument

class DriveWealthDocumentsRepo(
    storageHandler: GenericStorage,
    apiProvider: DriveWealthAccountApiProvider
) : GenericRepository<List<DriveWealthAccountDocument>, DriveWealthDocumentTypes, DriveWealthAccountApiProvider>(storageHandler, apiProvider) {

    override suspend fun fetchData(identifier: DriveWealthDocumentTypes?): List<DriveWealthAccountDocument>? {
        if (identifier == null) {
            logger.error("DriveWealthDocumentsRepo Identifier is null")
            return null
        }

        try {
            val response = apiProvider.getDriveWealthDocuments(identifier)

            if (response.responseType != BasicResponseTypes.Success || response.data == null) {
                return null
            }

            return getFromJson(mapOf("data" to response.data))
        } catch (ex: Exception) {
            logger.error("error occured trying to get drivewealth document $identifier", ex)
            return null
        }
    }

    override fun getFromJson(json: Map<String, Any>): List<DriveWealthAccountDocument> {
        val data = json["data"] as List<Map<String, Any>>
        return data.map { e -> DriveWealthAccountDocument.fromJson(e) }
    }

    override fun getIdentifier(data: List<DriveWealthAccountDocument>): DriveWealthDocumentTypes {
        throw NotImplementedError()
    }

    override fun getPath(identifier: DriveWealthDocumentTypes?): String {
        return "drivewealth_documents/${identifier?.apiPath}"
    }

    override fun toJson(data: List<DriveWealthAccountDocument>): Map<String, Any> {
        return mapOf("data" to data.map { e -> e.toJson() })
    }
}
