package sdk.trade.repositories.repos

import sdk.base.GenericRepository
import sdk.base.GenericStorage
import sdk.models.core.AssetProvider
import sdk.trade.GenericOrderAPIProvider
import sdk.trade.OrderData

abstract class OrdersRepository(
    storageHandler: GenericStorage,
    apiProvider: GenericOrderAPIProvider,
    val assetProvider: AssetProvider
) : GenericRepository<List<OrderData>, PaginatedOrdersFilter, GenericOrderAPIProvider>(storageHandler, apiProvider) {

    abstract suspend fun getOrderByID(id: String): OrderData?

    abstract fun orderDataFromJSON(json: Map<String, Any>): OrderData?
}

data class PaginatedOrdersFilter(
    val from: Int,
    val to: Int
)
