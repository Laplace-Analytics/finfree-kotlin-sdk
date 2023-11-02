package sdk.trade.repositories.repos

import sdk.base.GenericRepository
import sdk.base.GenericStorage
import sdk.models.data.assets.AssetId
import sdk.models.core.AssetProvider
import sdk.models.core.SessionProvider
import sdk.repositories.PriceDataRepo
import sdk.trade.GenericPortfolioApiProvider
import sdk.trade.OrderData
import sdk.trade.models.portfolio.PortfolioAssetData
import sdk.trade.models.portfolio.UserEquityData

abstract class UserEquityRepo<T : GenericPortfolioApiProvider>(
    storageHandler: GenericStorage,
    apiProvider: T,
    val priceDataRepo: PriceDataRepo,
    val sessionProvider: SessionProvider,
    val assetProvider: AssetProvider
) : GenericRepository<UserEquityData, PortfolioRepoIdentifier,  T>(storageHandler, apiProvider)

data class PortfolioRepoIdentifier(
    val orderData: List<OrderData>,
    val portfolioAssets: Map<AssetId, PortfolioAssetData>,
    val livePriceDataEnabled: Boolean = false
)
