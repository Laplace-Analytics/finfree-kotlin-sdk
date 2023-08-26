package sdk.models.core

import sdk.models.*
import sdk.models.core.sessions.DateTime
import java.time.*

fun HourMinuteSecond.getDateTime(date: LocalDateTime? = null): LocalDateTime {
    val date = date ?: LocalDateTime.now()

    var weekDaySinceMonday = (hour / 24)

    if (weekDaySinceMonday >= 0) {
        weekDaySinceMonday++
    }

    val realHour = hour % 24

    val resultingDateTime = LocalDateTime.of(
        date.year,
        date.month,
        date.dayOfMonth + (weekDaySinceMonday - date.dayOfWeek.value),
        realHour,
        minute,
        second
    )
    println(resultingDateTime)
    return resultingDateTime

}

class Sessions(
    val assetClass: AssetClass,
    val region: Region,
    points: List<HourMinuteSecond>
) {
    private val _points: List<HourMinuteSecond> = points

    fun isDuringMarketHours(date: LocalDateTime? = null): Boolean {
        return try {
            val hourMinuteSecond = HourMinuteSecond.fromDateTime(date ?: DateTime.now())
            val range = _getRange(hourMinuteSecond)
            _isDuringMarketHours(range, hourMinuteSecond)
        } catch (e: DateNotIncluded) {
            false
        }
    }

    private fun _isDuringMarketHours(
        range: List<HourMinuteSecond>,
        hourMinuteSecond: HourMinuteSecond
    ): Boolean {
        return (range[0] is OpenPoint && range[1] is ClosePoint) ||
                hourMinuteSecond.isEqual(date = range[0]) ||
                hourMinuteSecond.isEqual(date = range[1])
    }
    fun getDayStart(date: LocalDateTime? = null): HourMinuteSecond {
        return try {
            val hms = HourMinuteSecond.fromDateTime(date ?: DateTime.now())
            _getRange(hms).filterIsInstance<OpenPoint>().first()
        } catch (e: DateNotIncluded) {
            _getLastOpenPoint()
        } catch (ex: Exception) {
            // TODO check this implementation
            _points.first()
        }
    }

    fun getDayEnd(date: LocalDateTime? = null): HourMinuteSecond {
        return try {
            val hms = HourMinuteSecond.fromDateTime(date ?: DateTime.now())
            _getRange(hms).filterIsInstance<ClosePoint>().first()
        } catch (e: DateNotIncluded) {
            _getLastClosePoint()
        } catch (ex: Exception) {
            // TODO check this implementation
            _points[1]
        }
    }
    fun getPreviousClosestActiveDate(date: LocalDateTime? = null): LocalDateTime {
        return try {
            val hourMinuteSecond = HourMinuteSecond.fromDateTime(date ?: DateTime.now())
            val range = _getRange(hourMinuteSecond)

            if (_isDuringMarketHours(range, hourMinuteSecond)) {
                return date ?: DateTime.now()
            }

            return range.first().getDateTime(date = date)
        } catch (e: DateNotIncluded) {
            val localDate = date ?: DateTime.now()
            val isFirstDayOfWeek = localDate.dayOfWeek == DayOfWeek.MONDAY

            val lastPointHour = if (isFirstDayOfWeek && region == Region.american) {
                _points.last().hour - 144
            } else if (isFirstDayOfWeek) {
                _points.last().hour - 168
            } else {
                _points.last().hour
            }

            return HourMinuteSecond(
                hour = lastPointHour,
                minute = _points.last().minute,
                second = _points.last().second
            ).getDateTime(date = date)
        } catch (ex: Exception) {
            return _points.last().getDateTime(
                date = date?.minusDays(7)
            )
        }
    }

    fun getNextClosestActiveDate(date: LocalDateTime? = null): LocalDateTime {
        return try {
            val hourMinuteSecond = HourMinuteSecond.fromDateTime(date ?: DateTime.now())
            val range = _getRange(hourMinuteSecond)

            if (_isDuringMarketHours(range, hourMinuteSecond)) {
                return date ?: DateTime.now()
            }

            return range[1].getDateTime(date = date)
        } catch (e: DateNotIncluded) {
            val localDate = date ?: DateTime.now()
            val isFirstDayOfWeek = localDate.dayOfWeek == DayOfWeek.MONDAY

            val firstPointHour = if (isFirstDayOfWeek) {
                _points.first().hour
            } else {
                _points.first().hour + 168
            }

            return HourMinuteSecond(
                hour = firstPointHour,
                minute = _points.first().minute,
                second = _points.first().second
            ).getDateTime(date = date)
        } catch (ex: Exception) {
            return _points.first().getDateTime(
                date = date?.plusDays(7)
            )
        }
    }

    private fun _getLastOpenPoint(): HourMinuteSecond {
            for (i in _points.size - 1  downTo 0) {
                if (_points[i] is OpenPoint) {
                    return _points[i]
                }
            }
            throw DateNotFound()
    }

    private fun _getLastClosePoint(): HourMinuteSecond {
            for (i in _points.size - 1 downTo 0) {
                if (_points[i] is ClosePoint) {
                    return _points[i]
                }
            }
            throw DateNotFound()

    }

    private fun _getRange(hms: HourMinuteSecond): List<HourMinuteSecond> {
        for (i in _points.indices) {
            try {
                if (hms.isBetween(first = _points[i], second = _points[i + 1])) {
                    return listOf(_points[i], _points[i + 1])
                }
            } catch (e: IndexOutOfBoundsException) {
                throw DateNotIncluded()
            }
        }
        throw DateNotFound()
    }

    fun toJson(): Map<String, Any> {
        return mapOf(
            "region" to region.string(),
            "asset_class" to assetClass.string(),
            "points" to _points.map { it.toJson() }
        )
    }

    companion object {
        fun fromJson(json: Map<String, Any>): Sessions {
            return Sessions(
                assetClass = (json["asset_class"] as String).assetClass(),
                region = (json["region"] as String).region(),
                points = (json["points"] as List<Map<String, Any>>).map { HourMinuteSecond.fromJson(it) }
            )
        }
    }
}

class DateNotIncluded : Exception() {
    override fun toString(): String {
        return "date not found in sessions"
    }
}

class DateNotFound(override val message: String? = null) : Exception() {
    override fun toString(): String {
        return message?.let {
            "Date not found exception: $it"
        } ?: "Date not found exception."
    }
}

open class OpenPoint(
    hour: Int,
    minute: Int,
    second: Int
) : HourMinuteSecond(hour, minute, second) {

    companion object {
        fun fromDateTime(date: LocalDateTime): OpenPoint {
            return OpenPoint(
                hour = date.hour + ((date.dayOfWeek.value - 1) * 24),
                minute = date.minute,
                second = date.second
            )
        }

        fun fromJson(json: Map<String, Any>): OpenPoint {
            return OpenPoint(
                hour = json["hour"] as Int,
                minute = json["minute"] as Int,
                second = json["second"] as Int
            )
        }
    }

    override fun toJson(): Map<String, Any> {
        return mapOf(
            "hour" to hour,
            "minute" to minute,
            "second" to second,
            "type" to "open"
        )
    }
}
class ClosePoint(hour: Int, minute: Int, second: Int) : HourMinuteSecond(hour, minute, second) {

    companion object {
        fun fromDateTime(date: LocalDateTime): ClosePoint {
            val weekdayValue = date.dayOfWeek.value // 1 (Monday) to 7 (Sunday)
            val hour = date.hour + ((weekdayValue - 1) * 24)
            return ClosePoint(hour, date.minute, date.second)
        }

        fun fromJson(json: Map<String, Any>): ClosePoint {
            return ClosePoint(
                json["hour"] as Int,
                json["minute"] as Int,
                json["second"] as Int
            )
        }
    }

    override fun toJson(): Map<String, Any> {
        return mapOf(
            "hour" to hour,
            "minute" to minute,
            "second" to second,
            "type" to "close"
        )
    }
}

 open class HourMinuteSecond(
    val hour: Int,
    val minute: Int,
    val second: Int
) {

    companion object {
        fun fromDateTime(date: LocalDateTime): HourMinuteSecond {
            val weekdayValue = date.dayOfWeek.value // 1 (Monday) to 7 (Sunday)
            val hour = date.hour + ((weekdayValue - 1) * 24)
            return HourMinuteSecond(hour, date.minute, date.second)
        }

        fun fromJson(json: Map<String, Any>): HourMinuteSecond {
            when (json["type"]) {
                "close" -> return ClosePoint.fromJson(json)
                "open" -> return OpenPoint.fromJson(json)
                else -> return HourMinuteSecond(
                    hour = json["hour"] as Int,
                    minute = json["minute"] as Int,
                    second = json["second"] as Int
                )
            }
        }
    }
     open fun toJson(): Map<String, Any> = mapOf(
         "hour" to hour,
         "minute" to minute,
         "second" to second
     )

     override fun toString(): String = "hour : $hour - minute : $minute - second : $second"

     fun isBefore(hourMinuteSecond: HourMinuteSecond? = null, dateTime: LocalDateTime? = null): Boolean {
         val hms = hourMinuteSecond ?: fromDateTime(dateTime ?: DateTime.now())
         val compareHours = hour.compareTo(hms.hour)

         if (compareHours != 0) {
             return compareHours < 0
         }

         val compareMinutes = minute.compareTo(hms.minute)

         if (compareMinutes != 0) {
             return compareMinutes < 0
         }

         return second.compareTo(hms.second) < 0
     }
     fun isAfter(date: HourMinuteSecond? = null, dateTime: LocalDateTime? = null): Boolean {
         val hms = date ?: fromDateTime(dateTime ?: DateTime.now())

         if (isEqual(date = hms)) {
             return false
         }

         return !isBefore(hourMinuteSecond = hms)
     }

     fun isEqual(date: HourMinuteSecond? = null, dateTime: LocalDateTime? = null): Boolean {
         val hms = date ?: fromDateTime(dateTime ?: DateTime.now())

         return hms.hour == hour && hms.minute == minute && hms.second == second
     }
     fun isBetween(first: HourMinuteSecond? = null, second: HourMinuteSecond? = null): Boolean {
         val isEqualToAnyPoint = isEqual(date = first) || isEqual(date = second)

         if (isEqualToAnyPoint && first is ClosePoint && second is OpenPoint) {
             // TODO check here
             return false
         }

         return isEqualToAnyPoint || (isBefore(hourMinuteSecond = second) && isAfter(date = first))
     }

     fun props(): List<Int> = listOf(hour, minute, second)

     override fun equals(other: Any?): Boolean {
         if (this === other) return true
         if (other == null || this::class != other::class) return false

         other as HourMinuteSecond
         return props() == other.props()
     }


}

