package sdk.models.core

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sdk.base.logger
import sdk.models.*
import sdk.repositories.AssetCollectionRepo
import sdk.repositories.AssetCollectionRepoIdentifier
import sdk.repositories.AssetRepo
import sdk.repositories.AssetsRepoIdentifier
import java.time.Duration
import java.time.LocalDateTime

class AssetProvider(
    private val assetRepo: AssetRepo,
) {

    private val assetsById: MutableMap<Region, MutableMap<AssetId, Asset>> = mutableMapOf()

    // Holds symbols as key and assetId's as value.
    private val idsBySymbol: MutableMap<Region, MutableMap<AssetSymbol, AssetId>> = mutableMapOf()


    // Assumes a function "findBySymbol" is defined somewhere that returns an Asset?
    fun getAssetIdFromSymbol(symbol: AssetSymbol): AssetId? {
        return findBySymbol(symbol)?.id
    }

    // Assumes a function "findById" is defined somewhere that returns an Asset?
    fun findSymbolById(id: AssetId): AssetSymbol? {
        return findById(id)?.symbol
    }


    fun findById(id: AssetId): Asset? {
        if (!initialized) {
            return null
        }

        for (map in assetsById.values) {
            val asset = map[id]
            if (asset != null) {
                return asset
            }
        }
        return null
    }

    // Returns all assets by region
    fun getAssetsByRegion(region: Region): Map<AssetId, Asset>? {
        return assetsById[region]
    }

    fun findBySymbol(
        symbol: AssetSymbol,
        assetType: AssetType? = null,
        assetClass: AssetClass? = null,
        region: Region? = null
    ): Asset? {
        if (!initialized) {
            return null
        }

        for (map in idsBySymbol.values) {
            val id = map[symbol]
            if (id != null) {
                val asset = findById(id)
                if (asset != null) {
                    if (assetType != null && assetType != asset.type) {
                        continue
                    } else if (assetClass != null && assetClass != asset.assetClass) {
                        continue
                    } else if (region != null && region != asset.region) {
                        continue
                    } else {
                        return asset
                    }
                }
            }
        }
        return findById(symbol)
    }




    val allAssets: List<Asset>
        get() = assetsById.values.fold(
            mutableListOf(),
            { previousValue, element -> previousValue.apply { addAll(element.values) } }
        )

    val allSymbols: List<String>
        get() = idsBySymbol.values.fold(
            mutableListOf(),
            { previousValue, element -> previousValue.apply { addAll(element.keys) } }
        )

    val initialized: Boolean
        get() = assetsById.values.fold(
            true
        ) { previousValue, element -> previousValue && element.values.isNotEmpty() }



    suspend fun init(regions: Set<Region>) {
        val start = LocalDateTime.now()
        do {
            try {
                for (region in regions) {
                    assetsById[region] = mutableMapOf()
                    idsBySymbol[region] = mutableMapOf()
                }
                coroutineScope {
                    regions.forEach { region ->
                        launch { fetchStocks(region) }
                    }
                }
            } catch (ex: Exception) {
                logger.error("Error initializing assets, trying again in 500ms", ex)
                delay(500L)
            }
        } while (!initialized)
        logger.info("Assets initialized in ${Duration.between(start, LocalDateTime.now()).toMillis()} ms")
    }
    private suspend fun fetchStocks(region: Region) {

                if (assetsById[region]?.isNotEmpty() ?: false){
                    return
                }


        val stocksJson: List<Asset> = assetRepo.getData(
            AssetsRepoIdentifier(
            region =  region,
        ),)
            ?: throw Exception("Stocks cannot be fetched for region: $region")


        idsBySymbol.getOrPut(region) { mutableMapOf() }
        assetsById.getOrPut(region) { mutableMapOf() }

        for (asset in stocksJson) {
            idsBySymbol[region]!![asset.symbol] = asset.id
            assetsById[region]!![asset.id] = asset
        }
    }
}
