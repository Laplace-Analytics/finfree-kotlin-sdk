package sdk.base

import sdk.api.StockDataPeriods
import sdk.models.data.assets.Currency
import sdk.models.data.assets.currencySuffix
import sdk.models.data.assets.string
import java.text.NumberFormat
import java.time.Duration
import java.time.LocalDateTime
import java.util.Locale
import kotlin.math.absoluteValue
import kotlin.math.roundToInt


typealias DoubleFormatter = (number: Double, precision: Int?, suffix: String?, prefix: String?, nullValue: String?) -> String


fun getDoubleFromDynamic(value: Any?): Double? {
    when (value) {
        is Double -> return value
        is Int -> return value.toDouble()
        is String -> return value.toDoubleOrNull()
    }
    return null
}

fun getIntFromDynamic(value: Any?): Int? {
    when (value) {
        is Double -> return value.toInt()
        is Int -> return value
        is String -> return value.toIntOrNull()
    }
    return null
}

fun getOwnedStockCountText(quantity: Number): String {

    return when (quantity) {
        is Int -> quantity.toString()
        is Double -> {
            val precision = when {
                (quantity - quantity.roundToInt()).absoluteValue < 0.00001 -> 0
                (quantity - String.format(Locale.US,"%.1f", quantity).toDouble()).absoluteValue < 0.00001 -> 1
                (quantity - String.format(Locale.US,"%.2f", quantity).toDouble()).absoluteValue < 0.00001 -> 1
                else -> 3
            }
            return quantity.format(precision = precision)
        }
        else -> getOwnedStockCountText(quantity.toDouble())
    }
}


fun parseDoubleWithCommaDecimalSeparator(value: String?): Double? {
    return try {
        value?.replace(".", "")?.replace(",", ".")?.toDouble()
    } catch (e: NumberFormatException) {
        null
    }
}

fun formatLeaguePrize(prize: Double,currency: Currency): String {
    return prize.compactPrice(currency = currency)
}

fun formatFinancialValue(
    value: Double?,
    index: FinancialFormat?,
    nullValue: String = "-",
    adjust: Boolean = true
): String {
    if (value == null) {
        return nullValue
    } else if (index == null) {
        return value.format()
    }

    if (adjust && (value * index.multiplier).absoluteValue < 1 / Math.pow(10.0, index.precision.toDouble()) && value != 0.0) {
        val newVal = 1 / Math.pow(10.0, index.precision.toDouble()) / index.multiplier
        if (value > 0) {
            return formatFinancialValue(newVal, index, adjust = false)
        } else if (value < 0) {
            return formatFinancialValue(-newVal, index, adjust = false)
        }
    }

    val finalValue = value * index.multiplier

    return finalValue.formatSigned(
        suffix =  index.suffix,
    prefix =  index.prefix,
    precision =  if (finalValue > 1000 && (index.precision > 0)) 0 else index.precision
    )
}

fun dateIsBetween(start: LocalDateTime, end: LocalDateTime, check: LocalDateTime): Boolean {
    return check.isBefore(end) && check.isAfter(start)
}

fun turkishToEnglishChars(str: String): String {
    var queryFixed = str

    queryFixed = queryFixed.replace('ı', 'i')
    queryFixed = queryFixed.replace('ş', 's')
    queryFixed = queryFixed.replace('ç', 'c')
    queryFixed = queryFixed.replace('ö', 'o')
    queryFixed = queryFixed.replace('ü', 'u')
    queryFixed = queryFixed.replace('ğ', 'g')

    return queryFixed
}

fun getAllTimePeriod(start: LocalDateTime, end: LocalDateTime): StockDataPeriods {
    val difference = Duration.between(end, start).toDays()
    return when {
        difference < 2 -> StockDataPeriods.Price1D
        difference < 14 -> StockDataPeriods.Price1W
        difference < 60 -> StockDataPeriods.Price1M
        difference < 180 -> StockDataPeriods.Price3M
        difference < 365 * 2 -> StockDataPeriods.Price1Y
        else -> StockDataPeriods.Price5Y
    }
}

fun getTimePeriod(start: LocalDateTime, end: LocalDateTime): StockDataPeriods {
    val difference = Duration.between(end, start).toDays()
    return when {
        difference < 1 -> StockDataPeriods.Price1D
        difference < 7 -> StockDataPeriods.Price1W
        difference < 30 -> StockDataPeriods.Price1M
        difference < 90 -> StockDataPeriods.Price3M
        difference < 365 -> StockDataPeriods.Price1Y
        else -> StockDataPeriods.Price5Y
    }
}
typealias RepoGetFunction<T> = suspend () -> T?

suspend fun <T> tryNTimesIfResultIsNull(callback: RepoGetFunction<T>, tryLimit: Int = 3): T? {
    var tryCount = 0
    var response: T? = null

    while (tryCount < tryLimit && response == null) {
        try {
            response = callback()
        } catch (e: Exception) {
            logger.error("error occurred while in loop trying to call $callback $e")
            response = null
        } finally {
            tryCount++
            if (tryCount < tryLimit && response == null) {
                kotlinx.coroutines.delay(1000)
            }
        }
    }
    return response
}
fun String.withEnglishCharacters(): String {
    val from = "ÁÄÂÀÃÅČÇĆĎÉĚËÈÊẼĔȆĞÍÌÎÏİŇÑÓÖÒÔÕØŘŔŠŞŤÚŮÜÙÛÝŸŽáäâàãåčçćďéěëèêẽĕȇğíìîïıňñóöòôõøðřŕšşťúůüùûýÿžþÞĐđßÆa"
    val to = "AAAAAACCCDEEEEEEEEGIIIIINNOOOOOORRSSTUUUUUYYZaaaaaacccdeeeeeeeegiiiiinnooooooorrsstuuuuuyyzbBDdBAa"
    var res = this
    for (i in from.indices) {
        res = res.replace(from[i].toString(), to[i].toString())
    }
    return res
}

fun String.slug(): String {
    var str = this
    str = str.trim()
    str = str.lowercase()
    str = str.withEnglishCharacters()

    return str.replace(Regex("[^a-z0-9 -]"), "").replace(Regex("\\s+"), "-").replace(Regex("-+"), "-")
}

fun mod(a: Int, b: Int): Int {
    val result = a % b
    return if (result < 0) result + b else result
}
















