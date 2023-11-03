package sdk.base

import sdk.models.data.assets.Currency
import sdk.models.data.assets.abbreviation
import sdk.models.data.assets.currencySuffix
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.absoluteValue


val locale:String = "tr"
private const val decimalLimit: Int  = 6
private val thresholdRatio: Double  = 0.0001

fun Number?.format(
    precision: Int = 2,
    suffix: String? = null,
    prefix: String? = null,
    nullValue: String? = "-",
    adjustPrecision: Boolean = false
): String {
    if (this == null) {
        if (nullValue == null) {
            throw Exception("Number was null while trying to format")
        } else {
            return nullValue
        }
    }

    val adjustedPrecision = if (adjustPrecision) {
        getAdjustedPrecision(this, precision)
    } else {
        precision
    }

    val formattedValue = (if (prefix?.isNotEmpty() == true) "$prefix " else "") +
            NumberFormat.getInstance().apply {
                maximumFractionDigits = adjustedPrecision
                minimumFractionDigits = adjustedPrecision
            }.format(this) +
            (suffix ?: "")

    return formattedValue
}

fun Number?.formatSigned(
    showPositiveSign: Boolean = true,
    precision: Int = 2,
    suffix: String? = null,
    prefix: String? = null,
    nullValue: String? = "-",
    adjustPrecision: Boolean = false
): String {
    if (this == null) {
        if (nullValue == null) {
            throw Exception("Number was null while trying to format")
        } else {
            return nullValue
        }
    }

    val sign = if (this.toDouble() > 0) {
        if (showPositiveSign) "+" else ""
    } else {
        "-"
    }

    return sign + this.toDouble().absoluteValue.format(precision, suffix, prefix, nullValue, adjustPrecision)
}

fun Number?.compact(
    suffix: String? = null,
    prefix: String? = null,
    nullValue: String? = "-"
): String {
    if (this == null) {
        if (nullValue == null) {
            throw Exception("Number was null while trying to format")
        } else {
            return nullValue
        }
    }

    return (if (prefix?.isNotEmpty() == true) "$prefix " else "") +
            NumberFormat.getInstance().format(this) +
            (suffix ?: "")
}

fun Number?.formatPrice(
    currency: Currency,
    precision: Int = 2,
    prefix: String? = null,
    suffix: String? = null,
    nullValue: String? = "-",
    adjustPrecision: Boolean = false
): String {
    if (this == null) {
        if (nullValue == null) {
            throw Exception("Number was null while trying to format")
        } else {
            return nullValue
        }
    }

    val adjustedPrecision = if (adjustPrecision) {
        getAdjustedPrecision(this, precision)
    } else {
        precision
    }

    val format = NumberFormat.getCurrencyInstance(Locale(locale,"TR"))

    format.currency = java.util.Currency.getInstance(currency.abbreviation())

    format.maximumFractionDigits = adjustedPrecision
    format.minimumFractionDigits = adjustedPrecision

    return (if (prefix != null) "$prefix " else "") +
    format.format(this) +
            (suffix ?: "")

}

fun Number?.compactPrice(
    currency: Currency,
    precision: Int = 2,
    prefix: String? = null,
    suffix: String? = null,
    nullValue: String? = "-",
    adjustPrecision: Boolean = false
): String {
    if (this == null) {
        if (nullValue == null) {
            throw Exception("Number was null while trying to format")
        } else {
            return nullValue
        }
    }

    val adjustedPrecision = if (adjustPrecision) {
        getAdjustedPrecision(this, precision)
    } else {
        precision
    }

    val format = NumberFormat.getCompactNumberInstance(Locale(locale,"TR"),NumberFormat.Style.LONG)

    format.currency = java.util.Currency.getInstance(currency.abbreviation())

    format.maximumFractionDigits = adjustedPrecision
    format.minimumFractionDigits = adjustedPrecision


    return (if (prefix != null) "$prefix " else "") +
            format.format(this) + (suffix ?: "")
}

fun Number?.formatPercent(
    precision: Int = 2,
    adjustPrecision: Boolean = false,
    nullValue: String? = "-"
): String {
    if (this == null) {
        if (nullValue == null) {
            throw Exception("Number was null while trying to format")
        } else {
            return nullValue
        }
    }

    val adjustedPrecision = if (adjustPrecision) {
        getAdjustedPercentagePrecision(this, precision)
    } else {
        precision
    }

    return NumberFormat.getPercentInstance().apply {
        maximumFractionDigits = adjustedPrecision
        minimumFractionDigits = adjustedPrecision
    }.format(this.toDouble() / 100)

}


private fun getAdjustedPrecision(
    number: Number,
    precision: Int
): Int {
    var adjustedPrecision = precision
    if (number.toDouble() > 100) {
        adjustedPrecision = 1
    }
    if (number.toDouble() > 1000) {
        adjustedPrecision = 0
    }

    return adjustedPrecision
}

private fun getAdjustedPercentagePrecision(
    number: Number,
    precision: Int
): Int {
    var adjustedPrecision = precision

    if (number.toDouble() > 10) {
        adjustedPrecision = 1
    }
    if (number.toDouble() > 100) {
        adjustedPrecision = 0
    }

    return adjustedPrecision
}
