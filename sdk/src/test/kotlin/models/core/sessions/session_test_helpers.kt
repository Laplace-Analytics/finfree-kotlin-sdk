package models.core.sessions

import sdk.models.AssetClass
import sdk.models.Region
import java.time.DayOfWeek
import java.time.LocalDateTime

abstract class DateGetter {

    companion object {
        fun create(assetClass: AssetClass, region: Region): DateGetter {
            return when {
                assetClass == AssetClass.crypto -> TrCryptoEquityDateGetter()
                region == Region.american -> UsEquityDateGetter()
                else -> TrEquityDateGetter()
            }
        }
    }

    abstract fun dayStart(date: LocalDateTime): LocalDateTime
    abstract fun dayEnd(date: LocalDateTime): LocalDateTime
    abstract fun dayStartWithDifference(i: Int, date: LocalDateTime): LocalDateTime
    abstract fun dayEndWithDifference(i: Int, date: LocalDateTime): LocalDateTime
}

class TrEquityDateGetter : DateGetter() {
    override fun dayStart(date: LocalDateTime): LocalDateTime {
        return date.withHour(10).withMinute(0).withSecond(0)
    }

    override fun dayEnd(date: LocalDateTime): LocalDateTime {
        return date.withHour(18).withMinute(0).withSecond(0)
    }

    override fun dayStartWithDifference(i: Int, date: LocalDateTime): LocalDateTime {
        return date.plusDays(i.toLong()).withHour(10).withMinute(0).withSecond(0)
    }

    override fun dayEndWithDifference(i: Int, date: LocalDateTime): LocalDateTime {
        return date.plusDays(i.toLong()).withHour(18).withMinute(0).withSecond(0)
    }
}

class UsEquityDateGetter : DateGetter() {
    override fun dayStart(date: LocalDateTime): LocalDateTime {
        return date.withHour(17).withMinute(30).withSecond(0)
    }

    override fun dayEnd(date: LocalDateTime): LocalDateTime {
        return date.plusDays(1).withHour(0).withMinute(0).withSecond(0)
    }

    override fun dayStartWithDifference(i: Int, date: LocalDateTime): LocalDateTime {
        if (date.dayOfWeek == DayOfWeek.SATURDAY && date.hour == 0 && date.minute == 0 && date.second == 0) {
            return date
        } else {
            return date.plusDays(i.toLong()).withHour(17).withMinute(30).withSecond(0)
        }
    }

    override fun dayEndWithDifference(i: Int, date: LocalDateTime): LocalDateTime {
        if (date.dayOfWeek == DayOfWeek.SATURDAY && date.hour == 0 && date.minute == 0 && date.second == 0) {
            return date
        } else {
            return date.plusDays((i + 1).toLong()).withHour(0).withMinute(0).withSecond(0)
        }
    }

}

class TrCryptoEquityDateGetter : DateGetter() {
    override fun dayStartWithDifference(i: Int, date: LocalDateTime): LocalDateTime {
        return date
    }

    override fun dayEndWithDifference(i: Int, date: LocalDateTime): LocalDateTime {
        return date
    }

    override fun dayStart(date: LocalDateTime): LocalDateTime {
        return date
    }

    override fun dayEnd(date: LocalDateTime): LocalDateTime {
        return date
    }

    // Uncommented code for future reference
    /*
    override fun dayStart(date: LocalDateTime): LocalDateTime {
        return date.withHour(0).withMinute(0).withSecond(0)
    }

    override fun dayEnd(date: LocalDateTime): LocalDateTime {
        return date.plusDays(1).withHour(23).withMinute(59).withSecond(0)
    }

    override fun dayStartWithDifference(i: Int, date: LocalDateTime): LocalDateTime {
        return date.plusDays(i.toLong()).withHour(0).withMinute(0).withSecond(0)
    }

    override fun dayEndWithDifference(i: Int, date: LocalDateTime): LocalDateTime {
        return date.plusDays((i + 1).toLong()).withHour(23).withMinute(59).withSecond(0)
    }
    */
}

enum class DateStatus {
    InDay,
    AfterDay,
    BeforeDay
}

fun getDate(status: DateStatus, day: Int, region: Region, month: Int? = null, year: Int? = null): LocalDateTime {
    return when (region) {
        Region.turkish, Region.test -> {
            when (status) {
                DateStatus.BeforeDay -> LocalDateTime.of(year ?: 2021, month ?: 12, day, 8, 0)
                DateStatus.AfterDay -> LocalDateTime.of(year ?: 2021, month ?: 12, day, 21, 0)
                DateStatus.InDay -> LocalDateTime.of(year ?: 2021, month ?: 12, day, 12, 0)
            }
        }
        Region.american -> {
            when (status) {
                DateStatus.BeforeDay -> LocalDateTime.of(year ?: 2021, month ?: 12, day, 15, 0)
                DateStatus.AfterDay -> LocalDateTime.of(year ?: 2021, month ?: 12, day, 15, 30)
                DateStatus.InDay -> LocalDateTime.of(year ?: 2021, month ?: 12, day, 21, 0)
            }
        }
        else -> {
            when (status) {
                DateStatus.BeforeDay -> LocalDateTime.of(year ?: 2021, month ?: 12, day, 15, 0)
                DateStatus.AfterDay -> LocalDateTime.of(year ?: 2021, month ?: 12, day, 23, 58)
                DateStatus.InDay -> LocalDateTime.of(year ?: 2021, month ?: 12, day, 21, 0)
            }
        }
    }
}

fun compareToMarketHours(date: LocalDateTime, region: Region): Int {
    return when (region) {
        Region.american -> {
            if (date.hour == 0 && date.minute == 0 && date.second == 0 && listOf(2, 3, 4, 5, 6).any { it == date.dayOfWeek.value }) {
                0
            } else if (date.hour < 17 || (date.hour == 17 && date.minute < 30)) {
                -1
            } else if (date.hour > 23) {
                1
            } else {
                0
            }
        }
        Region.turkish -> {
            when {
                date.hour < 10 -> -1
                date.hour > 18 || (date.hour == 18 && date.minute > 0) -> 1
                else -> 0
            }
        }
        Region.test -> {
            if (date.dayOfWeek == DayOfWeek.SATURDAY) {
                when {
                    date.hour < 10 -> -1
                    date.hour > 13 || (date.hour == 13 && date.minute > 0) -> 1
                    else -> 0
                }
            } else {
                when {
                    date.hour < 10 -> -1
                    date.hour > 18 || (date.hour == 18 && date.minute > 30) -> 1
                    else -> 0
                }
            }
        }
        else -> 0
    }
}
val sessionsString = listOf(
    mapOf(
        "region" to "us",
        "asset_class" to "equity",
        "points" to listOf(
            mapOf("hour" to 17, "minute" to 30, "second" to 0, "type" to "open"),
            mapOf("hour" to 24, "minute" to 0, "second" to 0, "type" to "close"),
            mapOf("hour" to 41, "minute" to 30, "second" to 0, "type" to "open"),
            mapOf("hour" to 48, "minute" to 0, "second" to 0, "type" to "close"),
            mapOf("hour" to 65, "minute" to 30, "second" to 0, "type" to "open"),
            mapOf("hour" to 72, "minute" to 0, "second" to 0, "type" to "close"),
            mapOf("hour" to 89, "minute" to 30, "second" to 0, "type" to "open"),
            mapOf("hour" to 96, "minute" to 0, "second" to 0, "type" to "close"),
            mapOf("hour" to 113, "minute" to 30, "second" to 0, "type" to "open"),
            mapOf("hour" to 120, "minute" to 0, "second" to 0, "type" to "close")
        )
    ),
    mapOf(
        "region" to "tr",
        "asset_class" to "crypto",
        "points" to listOf(
            mapOf("hour" to 0, "minute" to 0, "second" to 0, "type" to "open"),
            mapOf("hour" to 23, "minute" to 59, "second" to 0, "type" to "close"),
            mapOf("hour" to 24, "minute" to 0, "second" to 0, "type" to "open"),
            mapOf("hour" to 47, "minute" to 59, "second" to 0, "type" to "close"),
            mapOf("hour" to 48, "minute" to 0, "second" to 0, "type" to "open"),
            mapOf("hour" to 71, "minute" to 59, "second" to 0, "type" to "close"),
            mapOf("hour" to 72, "minute" to 0, "second" to 0, "type" to "open"),
            mapOf("hour" to 95, "minute" to 59, "second" to 0, "type" to "close"),
            mapOf("hour" to 96, "minute" to 0, "second" to 0, "type" to "open"),
            mapOf("hour" to 119, "minute" to 59, "second" to 0, "type" to "close"),
            mapOf("hour" to 120, "minute" to 0, "second" to 0, "type" to "open"),
            mapOf("hour" to 143, "minute" to 59, "second" to 0, "type" to "close"),
            mapOf("hour" to 144, "minute" to 0, "second" to 0, "type" to "open"),
            mapOf("hour" to 167, "minute" to 59, "second" to 0, "type" to "close")
        )
    ),
    mapOf(
        "region" to "tr",
        "asset_class" to "equity",
        "points" to listOf(
            mapOf("hour" to 10, "minute" to 0, "second" to 0, "type" to "open"),
            mapOf("hour" to 18, "minute" to 0, "second" to 0, "type" to "close"),
            mapOf("hour" to 34, "minute" to 0, "second" to 0, "type" to "open"),
            mapOf("hour" to 42, "minute" to 0, "second" to 0, "type" to "close"),
            mapOf("hour" to 58, "minute" to 0, "second" to 0, "type" to "open"),
            mapOf("hour" to 66, "minute" to 0, "second" to 0, "type" to "close"),
            mapOf("hour" to 82, "minute" to 0, "second" to 0, "type" to "open"),
            mapOf("hour" to 90, "minute" to 0, "second" to 0, "type" to "close"),
            mapOf("hour" to 106, "minute" to 0, "second" to 0, "type" to "open"),
            mapOf("hour" to 114, "minute" to 0, "second" to 0, "type" to "close")
        )
    )
)




