package sdk.trade.repositories.drivewealth_repos

import sdk.base.GenericStorage
import sdk.base.logger
import sdk.base.network.BasicResponseTypes
import sdk.models.core.AssetProvider
import sdk.trade.GenericPortfolioApiProvider
import sdk.trade.models.portfolio.UserPortfolio
import sdk.trade.repositories.repos.UserPortfolioRepo

class DriveWealthUserPortfolioRepo(
    storageHandler: GenericStorage,
    apiProvider: GenericPortfolioApiProvider,
    assetProvider: AssetProvider
) : UserPortfolioRepo(storageHandler, apiProvider, assetProvider) {

    override suspend fun fetchData(identifier: Unit?): UserPortfolio? {
        return try {
            val response = apiProvider.getPortfolio()

            if (response.responseType == BasicResponseTypes.Error || response.data == null) {
                logger.error("${response.responseType} - ${response.message} - ${response.data}")
                return null
            }
            getFromJson(response.data)
        } catch (ex: Exception) {
            logger.error("Gedik US User stock data couldn't be fetched", ex)
            return null
        }
    }
    override fun getFromJson(json: Map<String, Any>): UserPortfolio {
        return UserPortfolio.fromJSON(json, assetProvider)
    }

    override fun getIdentifier(data: UserPortfolio) {
        throw NotImplementedError()
    }

    override fun getPath(identifier: Unit?): String {
        return "portfolio/user_portfolio"
    }

    override fun toJson(data: UserPortfolio): Map<String, Any> {
        return data.toJson()
    }

}
