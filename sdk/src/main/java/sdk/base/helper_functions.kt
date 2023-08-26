package sdk.base

import sdk.api.StockDataPeriods
import sdk.models.Currency
import sdk.models.currencySuffix
import sdk.models.string
import java.text.NumberFormat
import java.time.Duration
import java.time.LocalDateTime
import java.util.Locale
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

val decimalLimit = 6

typealias DoubleFormatter = (number: Double, precision: Int?, suffix: String?, prefix: String?, nullValue: String?) -> String

private fun getAdjustedPrecision(
    number: Double,
    precision: Int
): Int {
    var adjustedPrecision = precision
    if (number > 100) {
        adjustedPrecision = 1
    }
    if (number > 1000) {
        adjustedPrecision = 0
    }

    return adjustedPrecision
}

private fun getAdjustedPercentagePrecision(
    number: Double,
    precision: Int
): Int {
    var adjustedPrecision = precision

    if (number > 10) {
        adjustedPrecision = 1
    }
    if (number > 100) {
        adjustedPrecision = 0
    }

    return adjustedPrecision
}

fun formatDouble(
    number: Double?,
    precision: Int = 2,
    suffix: String? = null,
    currency: String? = null,
    prefix: String? = null,
    nullValue: String = "-",
    adjustPrecision: Boolean = false,
    adjustPercentagePrecision: Boolean = false
): String {
    if (number == null) {
        return nullValue
    }

    val adjustedPrecision = if (adjustPrecision) {
        getAdjustedPrecision(number, precision)
    } else {
        if (adjustPercentagePrecision) {
            getAdjustedPercentagePrecision(number, precision)
        } else {
            precision
        }
    }

    val format = NumberFormat.getInstance(Locale("eu","EU"))



    if (currency != null){
        format.currency = java.util.Currency.getInstance(currency)
    }
    if (adjustedPrecision == 0) {
        return "${prefix ?: ""} ${number.roundToInt()} ${currency?.let {format.currency.symbol} ?: ""}${suffix ?: ""}".trim()
    } else {
        format.maximumFractionDigits = adjustedPrecision
        format.minimumFractionDigits = adjustedPrecision
        return "${prefix ?: ""} ${format.format(number)} ${currency?.let {format.currency.symbol} ?: ""}${suffix ?: ""}".trim()
    }

}
fun formatCurrencyDouble(
    number: Double?,
    currency: Currency?,
    suffix: String?,
    prefix: String?,
    precision: Int = 2,
    nullValue: String = "-",
    adjustPrecision: Boolean = false
): String {
    if (number == null) {
        return nullValue
    }

    val adjustedPrecision = if (adjustPrecision) {
        getAdjustedPrecision(number, precision)
    } else {
        precision
    }

    val format = NumberFormat.getInstance(Locale("eu"))
    format.maximumFractionDigits = adjustedPrecision
    format.minimumFractionDigits = adjustedPrecision
    if (currency != null) {
        format.currency = java.util.Currency.getInstance(currency.string())
    }

    return "${prefix ?: ""} ${format.format(number)} ${suffix ?: ""}${currency?.currencySuffix() ?: ""}"
}

fun formatPriceDouble(
    number: Double? = null,
    precision: Int = 2,
    suffix: String? = null,
    prefix: String? = null,
    nullValue: String = "-",
    currency: Currency? = null,
    adjustPrecision: Boolean = false
): String {
    if (number == null) {
        return nullValue
    }

    val thresholdRatio = 0.0001

    if (number > 1.0) {
        return formatCurrencyDouble(
            number = number,
            precision = precision,
            nullValue = nullValue,
            prefix = prefix,
            suffix = suffix,
            adjustPrecision = true,
            currency = currency
        )
    }

    if (number == 0.0) {
        return formatCurrencyDouble(
            number = number,
            precision = 2,
            nullValue = nullValue,
            prefix = prefix,
            suffix = suffix,
            currency = currency,
            adjustPrecision = adjustPrecision
        )
    }

    for (decimalPoint in 0 until decimalLimit) {
        val nDecimalPoints = "%.${decimalPoint}f".format(Locale.US,number).toDouble()

        if (Math.abs((nDecimalPoints - number) / number) < thresholdRatio) {
            return formatCurrencyDouble(
                number = number,
                precision = if (decimalPoint < 2) 2 else decimalPoint,
                nullValue = nullValue,
                prefix = prefix,
                suffix = suffix,
                currency = currency,
                adjustPrecision = adjustPrecision
            )
        }
    }

    return formatCurrencyDouble(
        number = number,
        precision = decimalLimit,
        nullValue = nullValue,
        prefix = prefix,
        suffix = suffix,
        currency = currency,
        adjustPrecision = adjustPrecision
    )
}

fun getDoubleFromDynamic(value: Any?): Double? {
    when (value) {
        is Double -> return value
        is Int -> return value.toDouble()
        is String -> return value.toDoubleOrNull()
    }
    return null
}

fun getOwnedStockCountText(quantity: Number): String {
    println(quantity.toInt())

    return when (quantity) {
        is Int -> quantity.toString()
        is Double -> {
            val precision = when {
                (quantity - quantity.roundToInt()).absoluteValue < 0.00001 -> 0
                (quantity - String.format(Locale.US,"%.1f", quantity).toDouble()).absoluteValue < 0.00001 -> 1
                (quantity - String.format(Locale.US,"%.2f", quantity).toDouble()).absoluteValue < 0.00001 -> 1
                else -> 3
            }
            return formatDouble(quantity, precision = precision)
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

fun formatLeaguePrize(prize: Double): String {
    val precision: Int
    val multiplier: Double
    val suffix: String

    if (prize.roundToInt().toDouble() == prize) {
        if (prize >= 1000) {
            precision = when {
                (prize / 10) == (prize / 10).roundToInt().toDouble() -> 2
                (prize / 10) == (prize / 10).roundToInt().toDouble() -> 1
                else -> 0
            }
            multiplier = 1.0 / 1000.0
            suffix = "b ₺"
        } else {
            precision = 0
            multiplier = 1.0
            suffix = "₺"
        }
    } else {
        precision = 2
        multiplier = 1.0
        suffix = "₺"
    }

    val number = formatDouble(prize* multiplier, precision = precision, suffix = "")

    return number.substring(0, number.length) + suffix
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
        return formatDouble(value)
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

    return (if (finalValue >= 0) "" else "-") +
            formatDouble(
                finalValue.absoluteValue,
                suffix = index.suffix,
                prefix = index.prefix,
                precision = if (finalValue > 1000 && index.precision > 0) 0 else index.precision
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
















