package sdk.models.data.time_series

import sdk.api.StockDataPeriods
import sdk.api.getPeriodFromString
import sdk.models.*
import sdk.models.core.SessionProvider
import sdk.models.core.sessions.DateTime.Companion.toEpochMilliSecond
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.absoluteValue



class UserEquityTimeSeries(
    data: List<EquityDataPoint>,
    initialValue: Double,
    var period: StockDataPeriods,
    var currency: Currency = Currency.tl
) : TimeSeries<EquityDataPoint>(data, initialValue) {

    fun receiveDataPoint(newData: EquityDataPoint, sessionProvider: SessionProvider): UserEquityTimeSeries {
        val date = newData.time
        if (!sessionProvider.isDuringMarketHours(date = date)) {
            return this
        }
        val close = newData.value
        val aum = newData.assetsUnderManagement
        return if (data.isNotEmpty()) {
            val diff = Duration.between(data.last().time, date).toMinutes().absoluteValue

            when {
                period == StockDataPeriods.Price1D && data.last().time.atZone(ZoneId.systemDefault()).dayOfYear != date.atZone(ZoneId.systemDefault()).dayOfYear -> {
                    // Data point for a new day, so start a new timeSeries
                    UserEquityTimeSeries(
                        listOf(
                            EquityDataPoint(
                                index = 0,
                                timeStamp = date.toEpochMilliSecond(),
                                value = close,
                                assetsUnderManagement =aum,
                                realizedReturn = newData.realizedReturn,
                                unrealizedReturn = newData.unrealizedReturn
                            )
                        ), initialValue,period,  currency
                    )
                }
                period == StockDataPeriods.Price1D && diff >= period.periodMinutes -> {
                    // Add new data point
                    UserEquityTimeSeries(
                        data + listOf(
                            EquityDataPoint(
                                index = data.last().index + (diff / period.periodMinutes).toInt(),
                                timeStamp = date.toEpochMilliSecond(),
                                value = close,
                                assetsUnderManagement =aum,
                                realizedReturn = newData.realizedReturn,
                                unrealizedReturn = newData.unrealizedReturn
                            )
                        ),initialValue, period,  currency
                    )
                }
                else -> {
                    // Update the last data point
                    val updatedData = data.dropLast(1) + data.last().copyWith(
                        value = close,
                        assetsUnderManagement = aum,
                        realizedReturn = newData.realizedReturn,
                        unrealizedReturn = newData.unrealizedReturn
                    )
                    UserEquityTimeSeries(updatedData,initialValue, period,  currency)
                }
            }
        } else {
            UserEquityTimeSeries(
                listOf(
                    EquityDataPoint(
                        index = 0,
                        timeStamp = date.toEpochMilliSecond(),
                        value = close,
                        assetsUnderManagement =aum,
                        realizedReturn = newData.realizedReturn,
                        unrealizedReturn = newData.unrealizedReturn
                    )
                ), initialValue,period,  currency
            )
        }
    }

    fun multiplyWithPriceDataPoints(priceData: PriceDataSeries): UserEquityTimeSeries {
        val res = mutableListOf<EquityDataPoint>()

        var i = 0
        var j = 0
        var currI: EquityDataPoint
        var currJ: PriceDataPoint

        while (i < data.size) {
            currI = data[i]
            currJ = priceData.data[j]

            while (j < priceData.data.size - 1 && priceData.data[j + 1].time.isBefore(currI.time)) {
                j++
                currJ = priceData.data[j]
            }

            val newDataPoint = EquityDataPoint(
                index = currI.index,
                timeStamp = currI.time.toEpochMilliSecond(),
                assetsUnderManagement = currI.assetsUnderManagement * currJ.value,
                value = currI.value * currJ.value,
                realizedReturn = currI.realizedReturn * currJ.value,
                unrealizedReturn = currI.unrealizedReturn * currJ.value,
            )

            res.add(newDataPoint)
            i++
        }
        return UserEquityTimeSeries(
            res,
            initialValue * priceData.initialValue,
            period,
            currency = currency,
        )
    }

    val lastDataPointTime : LocalDateTime
        get() = if (data.isEmpty()) LocalDateTime.now() else data.last().time

    override val currentPrice: Double
        get() = if (data.isNotEmpty()) {
            data.last().assetsUnderManagement
        } else {
            initialValue
        }

    fun changeCurrency(exchangeRate: PriceDataSeries, isDivision: Boolean): UserEquityTimeSeries {
        if (exchangeRate.data.isEmpty()) {
            return this
        }

        val res = mutableListOf<EquityDataPoint>()

        var i = 0
        var j = 0
        var currI: EquityDataPoint
        var prevI: EquityDataPoint? = null
        var currJ: PriceDataPoint

        val baseOperation = { a: Double, b: Double -> if (isDivision) a / b else a * b }

        while (i < data.size) {
            currI = data[i]
            currJ = exchangeRate.data[j]

            while (j < exchangeRate.data.size - 1 && exchangeRate.data[j + 1].time.isBefore(currI.time)) {
                j++
                currJ = exchangeRate.data[j]
            }

            val newAssetsUnderManagement = baseOperation(currI.assetsUnderManagement, currJ.value)

            val currInv = currI.assetsUnderManagement - currI.returns
            val prevInv = prevI?.let { it.assetsUnderManagement - it.returns } ?: currInv

            val prevCalculatedInv = if (res.isEmpty()) prevInv / currJ.value else res.last().assetsUnderManagement - res.last().returns
            val currCalculatedInv = prevCalculatedInv + (currInv - prevInv) / currJ.value

            val newRealizedReturn = 0.0
            val newUnrealizedReturn = newAssetsUnderManagement - currCalculatedInv

            val newDataPoint = EquityDataPoint(
                assetsUnderManagement = newAssetsUnderManagement,
                value = baseOperation(currI.value, currJ.value),
                index = currI.index,
                timeStamp = currI.time.toEpochMilliSecond(),
                realizedReturn = newRealizedReturn,
                unrealizedReturn = newUnrealizedReturn
            )

            res.add(newDataPoint)
            i++
            prevI = currI
        }

        return UserEquityTimeSeries(
            res,
            baseOperation(initialValue, exchangeRate.initialValue),
            period,
            currency
        )
    }
      fun operation(
        baseFunction: (Double, Double) -> Double,
        other: TimeSeries<TimeSeriesDataPoint>
    ): UserEquityTimeSeries {
        if (other.data.isEmpty()) {
            return this
        }

        val res = mutableListOf<EquityDataPoint>()

        var i = 0
        var j = 0
         var currI: EquityDataPoint
         var currJ: TimeSeriesDataPoint

         val baseOperation = { a: Double, b: Double -> baseFunction(a, b) }

        while (i < data.size) {
            currI = data[i]
            currJ = other.data[j]

            while (j < other.data.size - 1 && other.data[j + 1].time.isBefore(currI.time)) {
                j++
                currJ = other.data[j]
            }

            var newAssetsUnderManagement: Double
            var newRealizedReturn: Double
            var newUnrealizedReturn: Double
            if (currJ is EquityDataPoint) {
                newAssetsUnderManagement = baseOperation(
                    currI.assetsUnderManagement,
                    currJ.assetsUnderManagement
                )
                newRealizedReturn = baseOperation(
                    currI.realizedReturn,
                    currJ.realizedReturn
                )
                newUnrealizedReturn = baseOperation(
                    currI.unrealizedReturn,
                    currJ.unrealizedReturn
                )
            } else {
                newAssetsUnderManagement = baseOperation(
                    currI.assetsUnderManagement,
                    currJ.value
                )
                newRealizedReturn = baseOperation(
                    currI.realizedReturn,
                    currJ.value
                )
                newUnrealizedReturn = baseOperation(
                    currI.unrealizedReturn,
                    currJ.value
                )
            }

            val newDataPoint = EquityDataPoint(
                assetsUnderManagement = newAssetsUnderManagement,
                value = baseOperation(currI.value, currJ.value),
                index = currI.index,
                timeStamp = currI.time.toEpochMilliSecond(),
                realizedReturn = newRealizedReturn,
                unrealizedReturn = newUnrealizedReturn
            )

            res.add(newDataPoint)
            i++
        }

        return UserEquityTimeSeries(
            res,
            baseOperation(initialValue, other.initialValue),
            period,
            currency
        )
    }
     override fun toJson(): Map<String, Any> {
        return mapOf(
            "data" to data.map { it.toJson() },
            "period" to period.period,
            "initialValue" to initialValue
        )
    }

    companion object {
        fun fromTimeSeries(timeSeries: TimeSeries<TimeSeriesDataPoint>, period: StockDataPeriods): UserEquityTimeSeries {
            return UserEquityTimeSeries(
                timeSeries.data as List<EquityDataPoint>,
                timeSeries.initialValue,
                period,
                )
        }

        fun fromJSON(json: Map<String, Any>): UserEquityTimeSeries {
            return UserEquityTimeSeries(
                (json["data"] as List<Map<String, Any>>).map { EquityDataPoint.fromJSON(it) },
                period = getPeriodFromString(json["period"] as String) ?: StockDataPeriods.Price1D,
                initialValue = json["initialValue"] as Double,
            )
        }
    }
    fun copyWith(
        data: List<EquityDataPoint>? = this.data,
        period: StockDataPeriods? = this.period,
        initialValue: Double? = this.initialValue
    ): UserEquityTimeSeries {
        return UserEquityTimeSeries(
            data = data ?: this.data,
            period = period ?: this.period,
            initialValue = initialValue ?: this.initialValue
        )
    }


}



class EquityDataPoint(
    val assetsUnderManagement: Double,
    val realizedReturn: Double,
    val unrealizedReturn: Double,
    index: Int,
    timeStamp: Long,
    value: Double
) : TimeSeriesDataPoint(index, timeStamp, value) {

    val returns get() = realizedReturn + unrealizedReturn

    override fun toString(): String {
        return "{index: $index - time: $time - value: $value - aum: $assetsUnderManagement}"
    }

    override fun toJson(): Map<String, Any> = mapOf(
        "d" to time.toEpochMilliSecond(),
        "index" to index,
        "indexed" to value,
        "aum" to assetsUnderManagement,
        "realized_return" to realizedReturn,
        "unrealized_return" to unrealizedReturn
    )

    companion object {
        fun fromJSON(data: Map<String, Any>): EquityDataPoint {
            return if (data.containsKey("aum")) {
                EquityDataPoint(
                    assetsUnderManagement = data["aum"] as Double,
                    index = data["index"] as Int,
                    value = data["indexed"] as Double,
                    timeStamp = data["d"] as Long,
                    realizedReturn = data["realized_return"] as? Double ?: 0.0,
                    unrealizedReturn = data["unrealized_return"] as? Double ?: 0.0
                )
            } else {
                EquityDataPoint(
                    assetsUnderManagement = data["value"] as Double,
                    index = data["index"] as Int,
                    value = data["value"] as Double,
                    timeStamp = data["d"] as? Long ?: data["timestamp"] as Long,
                    realizedReturn = data["realized_return"] as? Double ?: 0.0,
                    unrealizedReturn = data["unrealized_return"] as? Double ?: 0.0
                )
            }
        }
    }

    fun copyWith(
        assetsUnderManagement: Double? = null,
        index: Int? = null,
        value: Double? = null,
        timeStamp: Long? = null,
        realizedReturn: Double? = null,
        unrealizedReturn: Double? = null
    ): EquityDataPoint {
        return EquityDataPoint(
            assetsUnderManagement ?: this.assetsUnderManagement,
            realizedReturn ?: this.realizedReturn,
            unrealizedReturn ?: this.unrealizedReturn,
            index ?: this.index,
            timeStamp ?: this.time.toEpochMilliSecond(),
            value ?: this.value
        )
    }
}