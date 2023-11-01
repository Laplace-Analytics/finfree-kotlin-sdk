package sdk.trade.repositories.drivewealth_repos

import sdk.base.GenericRepository
import sdk.base.GenericStorage
import sdk.base.logger
import sdk.base.network.BasicResponseTypes
import sdk.trade.DriveWealthAccountApiProvider
import sdk.trade.models.portfolio.DriveWealthViolationsData

class DriveWealthViolationsRepo(
    storageHandler: GenericStorage,
    apiProvider: DriveWealthAccountApiProvider
) : GenericRepository<DriveWealthViolationsData, Unit, DriveWealthAccountApiProvider>(storageHandler, apiProvider) {

    override suspend fun fetchData(identifier: Unit?): DriveWealthViolationsData? {
        try {
            val response = apiProvider.getDriveWealthViolations()

            if (response.responseType != BasicResponseTypes.Success || response.data == null) {
                return null
            }

            return getFromJson(mapOf("data" to response.data))
        } catch (ex: Exception) {
            logger.error("error occurred trying to get drivewealth violations", ex)
            return null
        }
    }

    override fun getFromJson(json: Map<String, Any>): DriveWealthViolationsData {
        return DriveWealthViolationsData.fromJSON(json["data"] as Map<String, Any?>)
    }

    override fun toJson(data: DriveWealthViolationsData): Map<String, Any> {
        return data.toJson()
    }

    override fun getIdentifier(data: DriveWealthViolationsData): Unit {
        throw NotImplementedError()
    }

    override fun getPath(identifier: Unit?): String {
        return "drivewealth_violations"
    }
}
