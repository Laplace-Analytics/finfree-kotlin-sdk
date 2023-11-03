package sdk.models.data.assets

import sdk.base.GenericModel
import sdk.base.logger
import sdk.models.IndustryId
import sdk.models.SectorId

enum class Region {
    American,
    Turkish,
    Test
}

enum class AssetType {
    Stock,
    Etf,
    Crypto,
    Forex,
    Commodity
}

enum class AssetClass {
    Equity,
    Crypto,
    Forex
}

enum class Currency {
    Usd,
    Tl,
    Eur
}

enum class PortfolioType {
    Gedik,
    Virtual,
    DriveWealth,
}

enum class Content { TrEquity, UsEquity, TrCrypto }

val Asset.contentType: Content
    get() = getContentType(region, assetClass)

val Asset.notionalMarketOrderEnabled: Boolean
    get() = contentType == Content.UsEquity



fun getContentType(region: Region?, assetClass: AssetClass?): Content {
    if (region == null || assetClass == null) {
        throw Exception("Region or AssetClass is null: region: $region, assetClass: $assetClass")
    }
    when (region) {
        Region.Turkish -> {
            when (assetClass) {
                AssetClass.Equity -> return Content.TrEquity
                AssetClass.Crypto -> return Content.TrCrypto
                AssetClass.Forex -> {
                    // throw Exception("Forex is not supported in Turkey")
                }
            }
        }
        Region.American -> {
            when (assetClass) {
                AssetClass.Equity -> return Content.UsEquity
                AssetClass.Crypto -> {
                    // throw Exception("Crypto is not supported in US")
                }
                AssetClass.Forex -> {
                    // throw Exception("Forex is not supported in US")
                }
            }
        }
        Region.Test -> {
        }
    }
    throw Exception("Content type is not supported for given region and asset class: $region, $assetClass")
}

fun getMarketType(source:String): Region {
    return Region.Turkish
}

fun getDefaultAssetClass(): AssetClass {
    return AssetClass.Equity
}

fun AssetType.assetClass(): AssetClass {
    return when (this) {
        AssetType.Crypto -> AssetClass.Crypto
        AssetType.Stock -> AssetClass.Equity
        else -> getDefaultAssetClass()
    }
}

fun AssetClass.string(): String {
    return when (this) {
        AssetClass.Crypto -> "crypto"
        AssetClass.Forex -> "forex"
        AssetClass.Equity -> "equity"
    }
}

fun String.assetClass() : AssetClass {
    return when(this) {
        "crypto" -> AssetClass.Crypto
        "forex" -> AssetClass.Forex
        "equity" -> AssetClass.Equity
        else -> getDefaultAssetClass()
    }
}


fun Region.string(): String {
    return when (this) {
        Region.American -> "us"
        Region.Turkish -> "tr"
        Region.Test -> "test"
    }
}

fun Region.localeString(): String {
    return when (this) {
        Region.American -> "en"
        Region.Turkish -> "tr"
        Region.Test -> "test"
    }
}

fun Region.defaultCurrency(): Currency {
    return when (this) {
        Region.American -> Currency.Usd
        Region.Turkish -> Currency.Tl
        Region.Test -> Currency.Usd
    }
}

fun Region.currencySymbol(): String {
    return this.defaultCurrency().currencySuffix()
}

fun Region.priority(): Int {
    return when (this) {
        Region.Turkish -> 0
        Region.American -> 1
        else -> 999
    }
}

fun String.region(): Region {
    return when (this) {
        "us" -> Region.American
        "tr" -> Region.Turkish
        "test" -> Region.Test
        else -> throw IllegalArgumentException("Cannot get Region for String: \"$this\"")
    }
}

    fun AssetType.string(): String {
        return when (this) {
            AssetType.Stock -> "stock"
            AssetType.Crypto -> "crypto"
            AssetType.Etf -> "etf"
            AssetType.Forex -> "forex"
            AssetType.Commodity -> "commodity"
        }
    }

    fun String.assetType(): AssetType {
        return when (this) {
            "stock" -> AssetType.Stock
            "crypto" -> AssetType.Crypto
            "etf" -> AssetType.Etf
            "forex" -> AssetType.Forex
            "commodity" -> AssetType.Commodity
            else -> {
                logger.info("Cannot get Currency for String: \"$this\"")
                AssetType.Stock
            }
        }
    }


fun String.toCurrency(): Currency? {
    return when (this) {
        "₺" -> Currency.Tl
        "$" -> Currency.Usd
        else -> {
            println("Cannot get Currency for String: \"$this\"")
            null
        }
    }
}

fun getCurrencyByAbbreviation(abbreviation: String): Currency? {
    return when (abbreviation) {
        "TRY" -> Currency.Tl
        "USD" -> Currency.Usd
        "EUR" -> Currency.Eur
        else -> {
            println("Cannot get Currency for abbreviation: \"$abbreviation\"")
            null
        }
    }
}

fun Currency.currencySuffix(): String {
    return when (this) {
        Currency.Tl -> "₺"
        Currency.Usd -> "$"
        Currency.Eur -> "€"
    }
}

fun Currency.abbreviation(): String {
    return when (this) {
        Currency.Tl -> "TRY"
        Currency.Usd -> "USD"
        Currency.Eur -> "EUR"
    }
}

fun Currency.string(): String {
    return when (this) {
        Currency.Tl -> "TRY"
        Currency.Usd -> "USD"
        Currency.Eur -> "EUR"
    }
}

data class Asset(
    val id: AssetId,
    private val name: String?,
    val symbol: AssetSymbol,
    val sectorId: SectorId,
    val industryId: IndustryId,
    val isActive: Boolean,
    val region: Region,
    val type: AssetType,
    val tradable: Boolean
) : GenericModel {

    val assetClass: AssetClass
        get() = type.assetClass()

    val _name: String
        get() = name ?: symbol

    private val defaultCurrency: Currency
        get() = region.defaultCurrency()

    val currencySuffix: String
        get() = defaultCurrency.currencySuffix()

    val isTrEquity: Boolean
        get() = region == Region.Turkish && type == AssetType.Stock

    val isCrypto: Boolean
        get() = type == AssetType.Crypto

    val isPassive: Boolean
        get() = !isActive

    fun doubleTradable(isMarketOrder: Boolean): Boolean {
        return if (region == Region.American && (type == AssetType.Stock || type == AssetType.Etf)) {
            isMarketOrder
        } else {
            type == AssetType.Crypto
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Asset) return false

        return this.id == other.id &&
                this.name == other.name &&
                this.symbol == other.symbol &&
                this.sectorId == other.sectorId &&
                this.industryId == other.industryId &&
                this.isActive == other.isActive &&
                this.region == other.region &&
                this.type == other.type &&
                this.tradable == other.tradable
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "$symbol - $name - $type - $region - $id"
    }

    override fun toJson(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "name" to name,
            "symbol" to symbol,
            "sector_id" to sectorId,
            "industry_id" to industryId,
            "a" to isActive,
            "region" to region.string(),
            "asset_type" to type.string(),
            "tradable" to tradable
        )
    }

    companion object {
        fun fromJson(json: Map<String, Any?>): Asset {
            val regionString = json["region"] as String?
            val region = regionString?.region()
                ?: throw IllegalArgumentException("Region is null for asset: ${json["symbol"]}")
            return Asset(
                id = json["id"] as AssetId,
                name = json["name"] as String?,
                symbol = json["symbol"] as AssetSymbol,
                sectorId = json["sector_id"] as SectorId,
                industryId = json["industry_id"] as IndustryId,
                isActive = json["a"] as Boolean,
                region = region,
                type = (json["asset_type"] as String).assetType(),
                tradable = true /*json['tradable']*/
            )
        }
    }
}

typealias AssetId = String
typealias AssetSymbol = String
