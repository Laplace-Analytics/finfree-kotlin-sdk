package sdk.trade.repositories.repos

import sdk.base.GenericRepository
import sdk.base.GenericStorage
import sdk.models.core.AssetProvider
import sdk.trade.GenericPortfolioApiProvider
import sdk.trade.models.portfolio.UserPortfolio

abstract class UserPortfolioRepo<V>(
    storageHandler: GenericStorage,
    apiProvider: GenericPortfolioApiProvider,
    val assetProvider: AssetProvider
) : GenericRepository<UserPortfolio, V, GenericPortfolioApiProvider>(storageHandler, apiProvider)
