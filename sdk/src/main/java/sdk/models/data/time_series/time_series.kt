package sdk.models

import sdk.base.GenericModel
import sdk.models.core.sessions.DateTime
import sdk.models.core.sessions.DateTime.Companion.toEpochMilliSecond
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.*
import kotlin.math.absoluteValue

interface GenericDataPoint : GenericModel {
     val index: Int
     val time: LocalDateTime
     val value: Double
}

open class TimeSeriesDataPoint(
    override val index: Int,
    timeStamp: Long,
    override val value: Double
) : GenericDataPoint {



    override val time = DateTime.fromSinceEpochMilliSecond(timeStamp)
    override fun toString(): String {
        return "{$index - $time - $value}"
    }


    override fun toJson(): Map<String, Any> =
        mapOf("timestamp" to time.toEpochMilliSecond() , "index" to index, "value" to value)

    companion object {
        fun fromJson(data: Map<String, Any>): TimeSeriesDataPoint {
            return TimeSeriesDataPoint(data["index"] as Int, data["timestamp"] as Long, data["value"] as Double)
        }
    }
}

open class TimeSeries<T : TimeSeriesDataPoint>(var data: List<T>, val initialValue: Double) : GenericModel {

    var minValue: Double = Double.MAX_VALUE
    var maxValue: Double = Double.MIN_VALUE

    init {
        findMinMaxValues()
    }

    val absoluteChange: Double
        get() = currentPrice - initialValue

    val percentChange: Double
        get() = if (absoluteChange == 0.0) 0.0 else absoluteChange / Math.abs(initialValue) * 100

    open val currentPrice: Double
        get() = if (data.isNotEmpty()) data.last().value else initialValue

    operator fun plus(other: TimeSeries<T>): TimeSeries<T> {
        return operation({ a, b -> a + b }, other)
    }

    operator fun div(divider: TimeSeries<T>): TimeSeries<T> {
        return operation({ a, b -> a / b }, divider)
    }

    operator fun times(multiplier: TimeSeries<T>): TimeSeries<T> {
        return operation({ a, b -> a * b }, multiplier)
    }

    open fun multiplyBy(multiplier: Double, initialValueMultiplier: Double? = null): TimeSeries<T> {
        val newPoints = data.map { TimeSeriesDataPoint(it.index, it.time.toEpochMilliSecond(), it.value * multiplier) } as List<T>
        return TimeSeries(newPoints, initialValue * (initialValueMultiplier ?: multiplier))
    }

    open fun divideBy(divider: Double): TimeSeries<T> {
        if (divider == 0.0) {
            throw Exception("Division by zero")
        }
        return multiplyBy(1 / divider)
    }

    open fun operation(baseFunction: (Double, Double) -> Double, other: TimeSeries<T>): TimeSeries<T> {
        val newPoints = mutableListOf<TimeSeriesDataPoint>() as ArrayList<T>
        val baseOperation = { a: Double, b: Double -> baseFunction(a, b) }

        var i = 0
        var j = 0

        var currentI = data[i]
        var currentJ = other.data[j]

        while (i < data.size) {
            currentI = data[i]
            currentJ = other.data[j]

            while (j < other.data.size - 1 && other.data[j + 1].time.isBefore(currentI.time)) {
                j++
                currentJ = other.data[j]
            }

            val newValue = baseOperation(currentI.value, currentJ.value)
            val newDataPoint = TimeSeriesDataPoint(currentI.index, currentI.time.toEpochMilliSecond(), newValue) as T

            newPoints.add(newDataPoint)

            i++
        }

        return TimeSeries(newPoints, baseOperation(initialValue, other.initialValue))
    }

    override fun toJson(): Map<String, Any> {
        return mapOf("data" to data.map { it.toJson() }, "initialValue" to initialValue)
    }

    companion object {
        fun fromJson(json: Map<String, Any>): TimeSeries<TimeSeriesDataPoint> {
            val data = (json["data"] as List<Map<String, Any>>).map { TimeSeriesDataPoint.fromJson(it) }
            val initialValue = json["initialValue"] as Double
            return TimeSeries(data, initialValue)
        }
    }

     open fun findMinMaxValues() {
        if (data.isNotEmpty()) {
            minValue = data.minOf { it.value }
            maxValue = data.maxOf { it.value }
        }
    }

    override fun hashCode(): Int {
        return data.hashCode() + initialValue.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TimeSeries<*>) return false

        if (data != other.data) return false
        if (initialValue != other.initialValue) return false

        return true
    }
}


