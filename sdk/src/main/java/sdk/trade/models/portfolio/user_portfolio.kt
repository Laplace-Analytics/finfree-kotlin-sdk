package sdk.trade.models.portfolio

import sdk.base.GenericModel
import sdk.base.getDoubleFromDynamic
import sdk.models.data.assets.Asset
import sdk.models.data.assets.AssetClass
import sdk.models.data.assets.AssetId
import sdk.models.data.assets.AssetType
import sdk.models.data.assets.Region
import sdk.models.core.AssetProvider

class UserPortfolio(
    val portfolioAssets: MutableMap<AssetId, PortfolioAssetData>,
    val ipoStocks: List<IPOStockData> = emptyList()
) : GenericModel {

    override fun toString(): String {
        return portfolioAssets.entries.joinToString(",\n") { it.value.toString() }
    }

    fun buy(asset: Asset, amount: Number, price: Double) {
        if (portfolioAssets.containsKey(asset.id)) {
            portfolioAssets[asset.id] = portfolioAssets[asset.id]!!.buy(amount, price)
        } else {
            portfolioAssets[asset.id] = PortfolioAssetData(asset, amount, 0, price, price)
        }
    }

    fun sell(asset: Asset, amount: Number, price: Double) {
        if (portfolioAssets.containsKey(asset.id)) {
            portfolioAssets[asset.id] = portfolioAssets[asset.id]!!.sell(amount)
            if (portfolioAssets[asset.id]!!.quantity == 0) {
                portfolioAssets.remove(asset.id)
            }
        }
    }

    override fun toJson(): Map<String, Any> {
        return mapOf(
            "positions" to portfolioAssets.values.map { it.toJson() }
        )
    }

    companion object {
        fun fromJSON(json: Map<String, Any?>, assetProvider: AssetProvider): UserPortfolio {
            val ownedStocks = mutableMapOf<AssetId, PortfolioAssetData>()

            val positions: List<Map<String, Any>> = (json["positions"] as? List<Map<String, Any>>)?.toList() ?: emptyList()

            for (element in positions) {
                val reformattedJSON = mapOf(
                    "symbol" to element["symbol"],
                    "average_cost" to getDoubleFromDynamic(element["average_price"]),
                    "sellable" to getDoubleFromDynamic(element["sellable_qty"]),
                    "quantity" to getDoubleFromDynamic(element["open_qty"]),
                    "price" to getDoubleFromDynamic(element["last_sync_market_price"])
                )

                val asset = assetProvider.findBySymbol(
                    element["symbol"] as String,
                    assetClass = AssetClass.Equity,
                    region = Region.American
                )

                asset?.let {
                    ownedStocks[it.id] = PortfolioAssetData.fromJSON(reformattedJSON, it)
                }
            }

            val sortedOwnedStocks: MutableMap<AssetId, PortfolioAssetData> = mutableMapOf()

            val entries = ownedStocks.entries.toMutableList()
            entries.sortWith { a, b ->
                (b.value.quantity.toDouble() * b.value.currentPrice).compareTo(a.value.quantity.toDouble() * a.value.currentPrice)
            }

            for (element in entries) {
                sortedOwnedStocks[element.key] = element.value
            }

            return UserPortfolio(sortedOwnedStocks)
        }
    }
}

class PortfolioAssetData(
    val asset: Asset,
    val quantity: Number,
    val sellable: Number,
    val averagePrice: Double,
    val currentPrice: Double
) : GenericModel {
    val assetType: AssetType
        get() = asset.type

    override fun toString(): String {
        return "${asset.symbol}: $quantity"
    }

    fun buy(buyQuantity: Number, price: Double): PortfolioAssetData {
        val newAveragePriceTotal = averagePrice * quantity.toDouble() + price * buyQuantity.toDouble()
        val newQuantity = quantity.toDouble() + buyQuantity.toDouble()
        val newSellable = sellable.toDouble() + buyQuantity.toDouble()
        val newAveragePrice = newAveragePriceTotal / newQuantity

        return PortfolioAssetData(
            asset,
            newQuantity,
            newSellable,
            newAveragePrice,
            currentPrice
        )
    }

    fun sell(sellQuantity: Number): PortfolioAssetData {
        val newQuantity = quantity.toDouble() - sellQuantity.toDouble()
        var newSellable = sellable.toDouble()
        if (sellable.toDouble() >= sellQuantity.toDouble()) {
            newSellable -= sellQuantity.toDouble()
        }

        return PortfolioAssetData(
            asset,
            newQuantity,
            newSellable,
            averagePrice,
            currentPrice
        )
    }

    override fun toJson(): Map<String, Any> = mapOf(
        "stock_id" to asset.id,
        "quantity" to quantity,
        "sellable" to sellable,
        "average_price" to averagePrice,
        "price" to currentPrice
    )

    companion object {
        fun fromJSON(json: Map<String, Any?>, asset: Asset): PortfolioAssetData {
            return PortfolioAssetData(
                asset,
                json["quantity"] as Number,
                json["sellable"] as Number,
                (json["average_cost"] as Number)?.toDouble(),
                (json["price"] as Number)?.toDouble()
            )
        }
    }
}

data class IPOStockData(
    val symbol: String,
    val quantity: Int
) : GenericModel {

    override fun toJson(): Map<String, Any> {
        return mapOf(
            "symbol" to symbol,
            "quantity" to quantity
        )
    }

    companion object {
        fun fromJson(json: Map<String, Any>): IPOStockData {
            val quantityStr = json["quantity"]?.toString()
            val quantity = quantityStr?.toIntOrNull()

            if (quantity == null) throw Exception("Invalid quantity: $json")

            return IPOStockData(
                json["symbol"] as String,
                quantity
            )
        }
    }
}

