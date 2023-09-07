package sdk.models

import sdk.api.StockDataPeriods
import sdk.api.getPeriodFromString
import sdk.models.core.AssetProvider
import sdk.models.core.SessionProvider
import sdk.models.core.sessions.DateTime
import sdk.models.core.sessions.DateTime.Companion.toEpochMilliSecond
import java.lang.Double.max
import java.lang.Double.min
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.*

class PriceDataPoint(index: Int, timeStamp: Long, value: Double, val open: Double?, val low: Double?, val high: Double?, val volume: Int?)
    : TimeSeriesDataPoint(index, timeStamp, value) {

    override fun toString(): String {
        val localDateTime: LocalDateTime = time
        return "$value - ${localDateTime.minute}"
    }

    override fun toJson(): Map<String, Any> =   mapOf(
        "timestamp" to time.toEpochMilliSecond(),
        "index" to index,
        "close" to value,
        "open" to (open ?: 0.0),
        "low" to (low ?: 0.0),
        "high" to (high ?: 0.0),
        "volumeto" to (volume ?: 0.0)
    )

    companion object {
       fun fromJson(data: Map<String, Any>): PriceDataPoint {
            val index = data["index"] as Int
            val timeStamp = (data["timestamp"] as Number).toLong()
            val value = data["close"] as Double
            val open = (data["open"] ?: data["close"]) as Double?
           val low = data["low"] as? Double ?: data["close"] as Double
           val high = data["high"] as? Double ?: data["close"] as Double
            val volume = data["volumeto"] as Int?
            return PriceDataPoint(index, timeStamp, value, open, low, high, volume)
        }
    }

     fun copyWith(index: Int?= null, timeStamp: Long? = null, value: Double? = null, open: Double? = null, low: Double? = null, high: Double? = null, volume: Int? = null): PriceDataPoint {
        return PriceDataPoint(
            index ?: this.index,
            timeStamp ?: time.toEpochMilliSecond(),
            value ?: this.value,
            open ?: this.open,
            low ?: this.low,
            high ?: this.high,
            volume ?: this.volume
        )
    }
}

class PriceDataSeries(
    data: List<PriceDataPoint>,
    val asset: Asset,
    val period: StockDataPeriods,
    previousClose: Double
) : TimeSeries<PriceDataPoint>(data, previousClose) {
    init {
        findMinMaxValues()
    }

    val region get() = asset.region
    val assetClass get() = asset.assetClass

    override fun toString(): String {
        return "${asset.id}, ${asset.region}, $period: ${data.joinToString { it.toString() }}"
    }

    fun receiveDataPoint(
        isLiveDataActive: Boolean,
        sessionProvider: SessionProvider,
        close: Double,
        open: Double? = null,
        low: Double? = null,
        high: Double? = null,
        volume: Int? = null
    ): PriceDataSeries {
        val date = if (isLiveDataActive) DateTime.now() else DateTime.delayed()
        if (!sessionProvider.isDuringMarketHours(region, assetClass,date)) {
            return this
        }
        if (data.isNotEmpty()) {
            val diff = Duration.between(data.last().time, date).toMinutes().toInt()
            var lastDataPoint = data.last()
            if (lastDataPoint.time.dayOfMonth != date.dayOfMonth && period == StockDataPeriods.Price1D) {
                return PriceDataSeries(
                    mutableListOf(PriceDataPoint(0, date.toEpochMilliSecond(), close, open, low, high, volume)),
                    asset,
                    period,
                    initialValue
                )
            } else if (diff >= period.periodMinutes) {
                return PriceDataSeries(
                    data + PriceDataPoint(
                        lastDataPoint.index + (diff / period.periodMinutes),
                        date.toEpochMilliSecond(),
                        close,
                        open ?: close,
                        low ?: close,
                        high ?: close,
                        volume ?: 0
                    ),
                    asset,
                    period,
                    initialValue
                )
            } else {
                // update last data point
                lastDataPoint = lastDataPoint.copyWith(
                    value = close,
                    low = min(lastDataPoint.low ?: close, low ?: close),
                    high = max(lastDataPoint.high ?: close, high ?: close),
                    volume = (lastDataPoint.volume ?: 0) + (volume ?: 0)
                )
                return PriceDataSeries(data, asset, period, initialValue)
            }
        } else {
            return PriceDataSeries(
                mutableListOf(PriceDataPoint(0, date.toEpochMilliSecond(), close, open, low, high, volume)),
                asset,
                period,
                initialValue
            )
        }
    }

       override fun multiplyBy(multiplier: Double, initialValueMultiplier: Double?): PriceDataSeries {
        if (data.isEmpty()) {
            return this
        }

        val newDataPoints = data.map {
            it.copyWith(
                value = it.value * multiplier,
                open = it.open?.let { open -> open * multiplier },
                low = it.low?.let { low -> low * multiplier },
                high = it.high?.let { high -> high * multiplier }
            )
        }

        return PriceDataSeries(
            newDataPoints.toList(),
            asset,
            period,
            initialValue * (initialValueMultiplier ?: multiplier)
        )
    }
     override fun divideBy(divider: Double): PriceDataSeries {
        if (divider == 0.0) {
            throw IllegalArgumentException("Division by zero")
        }
        return multiplyBy(1 / divider)
    }

     fun operation(baseFunction: (Double, Double) -> Double, other: TimeSeries<TimeSeriesDataPoint>): PriceDataSeries {
        if (other.data.isEmpty()) {
            return this
        }

        val res = mutableListOf<PriceDataPoint>()

        var i = 0
        var j = 0

         var currI: PriceDataPoint
         var currJ: TimeSeriesDataPoint

         fun baseOperation(a: Double, b: Double) = baseFunction(a, b)

        while (i < data.size) {
            currI = data[i]
            currJ = other.data[j]

            while (j < other.data.size - 1 && other.data[j + 1].time.isBefore(currI.time)) {
                j++
                currJ = other.data[j]
            }

            val newDataPoint = PriceDataPoint(
                currI.index,
                currI.time.toEpochMilliSecond(),
                baseOperation(currI.value, currJ.value),
                baseOperation(currI.open ?: currI.value, currJ.value),
                baseOperation(currI.low ?: currI.value, currJ.value),
                baseOperation(currI.high ?: currI.value, currJ.value),
                currI.volume
            )

            res.add(newDataPoint)
            i++
        }

        return PriceDataSeries(
            res,
            asset,
            period,
            baseOperation(initialValue, other.initialValue)
        )
    }

     override fun toJson(): Map<String, Any> {
        return mapOf(
            "asset_id" to asset.id,
            "period" to period.period,
            "data" to data.map { it.toJson() },
            "initialValue" to initialValue
        )
    }

    // static function to create PriceDataSeries from a Map (similar to Dart's factory constructor)
    companion object {
        fun fromJSON(json: Map<String, Any>, assetProvider: AssetProvider): PriceDataSeries {
            val asset = assetProvider.findById(json["asset_id"] as String)
                ?: throw IllegalArgumentException("Asset with id ${json["asset_id"]} not found")
            val data = (json["data"] as List<Map<String, Any>>).map { PriceDataPoint.fromJson(it) }
            return PriceDataSeries(
                data,
                asset,
                getPeriodFromString(json["period"] as String) ?: StockDataPeriods.Price1D,
                json["initialValue"] as Double)
        }
    }

    override fun findMinMaxValues(
    ) {
        var minVal = if (data.isNotEmpty()) data.first().value else initialValue
        var maxVal = minVal

        for (dataPoint in data) {
            minVal = min(minVal, dataPoint.low ?: dataPoint.value)
            maxVal = max(maxVal, dataPoint.high ?: dataPoint.value)
        }
        minValue = minVal
        maxValue = maxVal
    }

     override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PriceDataSeries) return false
        if (asset.id != other.asset.id) return false
        if (region != other.region) return false
        if (data != other.data) return false
        if (period != other.period) return false
        return true
    }

}

