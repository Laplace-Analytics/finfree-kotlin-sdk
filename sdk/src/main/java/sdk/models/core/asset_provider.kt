package sdk.models.core

import jdk.internal.net.http.common.Log
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sdk.base.logger
import sdk.models.*
import sdk.repositories.AssetCollectionRepo
import sdk.repositories.AssetCollectionRepoIdentifier
import sdk.repositories.AssetRepo
import java.time.Duration
import java.time.LocalDateTime

class AssetProvider(
    private val assetRepo: AssetRepo,
    private val assetCollectionRepo: AssetCollectionRepo
) {

    private val assetsById: MutableMap<Region, MutableMap<AssetId, Asset>> = mutableMapOf()

    // Holds symbols as key and assetId's as value.
    private val idsBySymbol: MutableMap<Region, MutableMap<AssetSymbol, AssetId>> = mutableMapOf()

    private val sectors: MutableMap<Region, MutableMap<SectorId, AssetCollection>> = mutableMapOf()
    private val industries: MutableMap<Region, MutableMap<IndustryId, AssetCollection>> = mutableMapOf()
    private val collections: MutableMap<Region, MutableMap<CollectionId, AssetCollection>> = mutableMapOf()

    // Assumes a function "findBySymbol" is defined somewhere that returns an Asset?
    fun getAssetIdFromSymbol(symbol: AssetSymbol): AssetId? {
        return findBySymbol(symbol)?.id
    }

    // Assumes a function "findById" is defined somewhere that returns an Asset?
    fun findSymbolById(id: AssetId): AssetSymbol? {
        return findById(id)?.symbol
    }


    fun findById(id: AssetId): Asset? {
        if (!assetsInitialized) {
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
        if (!assetsInitialized) {
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

    fun findIndustryById(id: AssetId): AssetCollection? {
        if (!industriesInitialized) return null
        for (map in industries.values) {
            val collection = map[id]
            if (collection != null) {
                return collection
            }
        }
        return null
    }

    /**
     * Returns the asset collection includes assets for the given sector id.
     * Returns null if the sector cannot be found.
     */
    fun findSectorById(id: AssetId): AssetCollection? {
        if (!sectorsInitialized) return null
        for (map in sectors.values) {
            val collection = map[id]
            if (collection != null) {
                return collection
            }
        }
        return null
    }

    fun findCollectionById(id: AssetId): AssetCollection? {
        if (!collectionsInitialized) return null
        for (map in collections.values) {
            val collection = map[id]
            if (collection != null) {
                return collection
            }
        }
        return null
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

    val allSectors: List<AssetCollection>
        get() = sectors.values.fold(
            mutableListOf(),
            { previousValue, element -> previousValue.apply { addAll(element.values) } }
        )

    val allIndustries: List<AssetCollection>
        get() = industries.values.fold(
            mutableListOf(),
            { previousValue, element -> previousValue.apply { addAll(element.values) } }
        )

    val allCollections: List<AssetCollection>
        get() = collections.values.fold(
            mutableListOf(),
            { previousValue, element -> previousValue.apply { addAll(element.values) } }
        )

    // Returns the all Assets for the given Region
    fun assetsForRegion(region: Region): Map<AssetId, Asset>? = assetsById[region]

    // Returns the all industry AssetCollection for the given Region
    fun industriesForRegion(region: Region): Map<IndustryId, AssetCollection>? = industries[region]

    // Returns the all sector AssetCollection for the given Region
    fun sectorsForRegion(region: Region): Map<SectorId, AssetCollection>? = sectors[region]
    val initialized: Boolean
        get() = assetsInitialized && sectorsInitialized && industriesInitialized && collectionsInitialized

    val assetsInitialized: Boolean
        get() = assetsById.values.fold(
            true,
            { previousValue, element -> previousValue && element.values.isNotEmpty() }
        )

    val sectorsInitialized: Boolean
        get() = sectors.values.fold(
            true,
            { previousValue, element -> previousValue && element.values.isNotEmpty() }
        )

    val industriesInitialized: Boolean
        get() = industries.values.fold(
            true,
            { previousValue, element -> previousValue && element.values.isNotEmpty() }
        )

    val collectionsInitialized: Boolean
        get() = collections.values.fold(
            true,
            { previousValue, element -> previousValue && element.values.isNotEmpty() }
        )

    suspend fun init(regions: Set<Region>) {


        val start = LocalDateTime.now()
        do {
            try {
                for (region in regions) {
                    assetsById[region] = mutableMapOf()
                    idsBySymbol[region] = mutableMapOf()
                    sectors[region] = mutableMapOf()
                    industries[region] = mutableMapOf()
                    collections[region] = mutableMapOf()
                }
                coroutineScope {
                    regions.forEach { region ->
                        launch { fetchStocks(region) }
                        launch { fetchSectors(region) }
                        launch { fetchIndustries(region) }
                        launch { fetchCollections(region) }
                    }
                }
            } catch (ex: Exception) {
                logger.error("Error initializing assets, trying again in 500ms", ex)
                delay(500L)
            }
        } while (!initialized)
        jdk.internal.org.jline.utils.Log.info("Assets initialized in ${Duration.between(start, LocalDateTime.now()).toMillis()} ms")
    }
    private suspend fun fetchStocks(region: Region) {

                if (assetsById[region]?.isNotEmpty() ?: false){
                    return
                }


        val stocksJson: List<Asset> = assetRepo.getData(region)
            ?: throw Exception("Stocks cannot be fetched for region: $region")


        idsBySymbol.getOrPut(region) { mutableMapOf() }
        assetsById.getOrPut(region) { mutableMapOf() }

        for (asset in stocksJson) {
            idsBySymbol[region]!![asset.symbol] = asset.id
            assetsById[region]!![asset.id] = asset
        }
    }

    private suspend fun fetchSectors(region: Region) {
        if(sectors[region]?.isNotEmpty() ?: false){
            return
        }

        val collections: List<AssetCollection> = assetCollectionRepo.getData(
            AssetCollectionRepoIdentifier(region, CollectionType.sector)
        ) ?: throw Exception("Sectors could not be fetched for region: $region")

        sectors.getOrPut(region) { mutableMapOf() }

        for (sector in collections) {
            sectors[region]!![sector.id] = sector
        }
    }
    private suspend fun fetchCollections(region: Region) {
        if (collections[region]?.isNotEmpty() ?: false) {
            return
        }

        val _collections = assetCollectionRepo.getData(
            AssetCollectionRepoIdentifier(region, CollectionType.collection)
        ) ?: throw Exception("Collections could not be fetched for region: $region")

        collections.getOrPut(region) { mutableMapOf() }

        for (collection in _collections) {
            collections[region]!![collection.id] = collection
        }
    }

   private suspend fun fetchIndustries(region: Region) {
        if (industries[region]?.isNotEmpty() ?: false) {
            return
        }

        val collections = assetCollectionRepo.getData(
            AssetCollectionRepoIdentifier(region, CollectionType.industry)
        ) ?: throw Exception("Industries could not be fetched for region: $region")

       industries.getOrPut(region) { mutableMapOf() }

        for (industry in collections) {
            industries[region]!![industry.id] = industry
        }
    }

}
