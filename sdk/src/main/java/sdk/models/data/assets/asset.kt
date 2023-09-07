package sdk.models

import sdk.base.GenericModel
import sdk.base.logger

enum class Region {
    american,
    turkish,
    test
}

enum class AssetType {
    stock,
    etf,
    crypto,
    forex,
    commodity
}

enum class AssetClass {
    equity,
    crypto,
    forex
}

enum class Currency {
    usd,
    tl,
    eur
}

enum class PortfolioType {
    RealPortfolio,
    VirtualPortfolio,
    DriveWealthPortfolio,
}
enum class Content { TrEquity, UsEquity, TrCrypto }

val Asset.contentType: Content?
    get() = getContentType(region, assetClass)

val Asset.notionalMarketOrderEnabled: Boolean
    get() = contentType == Content.UsEquity



fun getContentType(region: Region?, assetClass: AssetClass?): Content? {
    if (region == null || assetClass == null) {
        return null
    }
    when (region) {
        Region.turkish -> {
            when (assetClass) {
                AssetClass.equity -> return Content.TrEquity
                AssetClass.crypto -> return Content.TrCrypto
                AssetClass.forex -> {
                    // throw Exception("Forex is not supported in Turkey")
                }
            }
        }
        Region.american -> {
            when (assetClass) {
                AssetClass.equity -> return Content.UsEquity
                AssetClass.crypto -> {
                    // throw Exception("Crypto is not supported in US")
                }
                AssetClass.forex -> {
                    // throw Exception("Forex is not supported in US")
                }
            }
        }
        Region.test -> {
        }
    }
    logger.error("Content type is not supported for given region and asset class: $region, $assetClass")
    return null
}

fun getMarketType(source:String):Region {
    return Region.turkish
}

fun getDefaultAssetClass(): AssetClass {
    return AssetClass.equity
}

fun AssetType.assetClass(): AssetClass {
    return when (this) {
        AssetType.crypto -> AssetClass.crypto
        AssetType.stock -> AssetClass.equity
        else -> getDefaultAssetClass()
    }
}

fun AssetClass.string(): String {
    return when (this) {
        AssetClass.crypto -> "crypto"
        AssetClass.forex -> "forex"
        AssetClass.equity -> "equity"
    }
}

fun String.assetClass() : AssetClass {
    return when(this) {
        "crypto" -> AssetClass.crypto
        "forex" -> AssetClass.forex
        "equity" -> AssetClass.equity
        else -> getDefaultAssetClass()
    }
}


fun Region.string(): String {
    return when (this) {
        Region.american -> "us"
        Region.turkish -> "tr"
        Region.test -> "test"
    }
}

fun Region.localeString(): String {
    return when (this) {
        Region.american -> "en"
        Region.turkish -> "tr"
        Region.test -> "test"
    }
}

fun Region.defaultCurrency(): Currency {
    return when (this) {
        Region.american -> Currency.usd
        Region.turkish -> Currency.tl
        Region.test -> Currency.usd
    }
}

fun Region.currencySymbol(): String? {
    return this.defaultCurrency().currencySuffix()
}

fun Region.priority(): Int {
    return when (this) {
        Region.turkish -> 0
        Region.american -> 1
        else -> 999
    }
}

fun String.region(): Region {
    return when (this) {
        "us" -> Region.american
        "tr" -> Region.turkish
        "test" -> Region.test
        else -> throw IllegalArgumentException("Cannot get Region for String: \"$this\"")
    }
}

    fun AssetType.string(): String {
        return when (this) {
            AssetType.stock -> "stock"
            AssetType.crypto -> "crypto"
            AssetType.etf -> "etf"
            AssetType.forex -> "forex"
            AssetType.commodity -> "commodity"
        }
    }

    fun String.assetType(): AssetType {
        return when (this) {
            "stock" -> AssetType.stock
            "crypto" -> AssetType.crypto
            "etf" -> AssetType.etf
            "forex" -> AssetType.forex
            "commodity" -> AssetType.commodity
            else -> {
                logger.info("Cannot get Currency for String: \"$this\"")
                AssetType.stock
            }
        }
    }


fun String.toCurrency(): Currency? {
    return when (this) {
        "₺" -> Currency.tl
        "$" -> Currency.usd
        else -> {
            println("Cannot get Currency for String: \"$this\"")
            null
        }
    }
}

fun getCurrencyByAbbreviation(abbreviation: String): Currency? {
    return when (abbreviation) {
        "TRY" -> Currency.tl
        "USD" -> Currency.usd
        "EUR" -> Currency.eur
        else -> {
            println("Cannot get Currency for abbreviation: \"$abbreviation\"")
            null
        }
    }
}

fun Currency.currencySuffix(): String {
    return when (this) {
        Currency.tl -> "₺"
        Currency.usd -> "$"
        Currency.eur -> "€"
    }
}

fun Currency.abbreviation(): String {
    return when (this) {
        Currency.tl -> "TRY"
        Currency.usd -> "USDTRY"
        Currency.eur -> "EUR"
    }
}

fun Currency.string(): String {
    return when (this) {
        Currency.tl -> "TRY"
        Currency.usd -> "USD"
        Currency.eur -> "EUR"
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

    val defaultCurrency: Currency
        get() = region.defaultCurrency()

    val currencySuffix: String
        get() = defaultCurrency.currencySuffix()

    val isTrEquity: Boolean
        get() = region == Region.turkish && type == AssetType.stock

    val isCrypto: Boolean
        get() = type == AssetType.crypto

    val isPassive: Boolean
        get() = !isActive

    fun doubleTradable(isMarketOrder: Boolean): Boolean {
        return if (region == Region.american && (type == AssetType.stock || type == AssetType.etf)) {
            isMarketOrder
        } else {
            type == AssetType.crypto
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Asset) return false

        val otherAsset = (other as Asset)

        return this.id == otherAsset.id &&
                this.name == otherAsset.name &&
                this.symbol == otherAsset.symbol &&
                this.sectorId == otherAsset.sectorId &&
                this.industryId == otherAsset.industryId &&
                this.isActive == otherAsset.isActive &&
                this.region == otherAsset.region &&
                this.type == otherAsset.type &&
                this.tradable == otherAsset.tradable
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
