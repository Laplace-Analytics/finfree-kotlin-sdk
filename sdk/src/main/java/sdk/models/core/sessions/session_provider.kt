package sdk.models.core

import kotlinx.coroutines.delay
import sdk.api.StockDataPeriods
import sdk.models.AssetClass
import sdk.models.FinancialQuarter
import sdk.models.Region
import sdk.repositories.SessionsRepo
import java.time.Duration
import java.time.LocalDateTime

class SessionProvider(private val sessionsRepo: SessionsRepo) {

    private val _sessions = mutableMapOf<AssetClass, MutableMap<Region, Sessions>>()

    private var _defaultLocation = Region.turkish
    private val _defaultAsset = AssetClass.equity

    private fun _getSession(region: Region, assetClass: AssetClass): Sessions {
        return _sessions[assetClass]?.get(region)
            ?: _sessions[AssetClass.equity]?.get(Region.turkish)
            ?: _sessions.values.first().values.first()
    }

    fun setDefaultLocation(region: Region) {
        _defaultLocation = region
    }

    private val _initialized: Boolean
        get() = _sessions.isNotEmpty()

    fun getDayStart(region: Region? = null, assetClass: AssetClass? = null, date: LocalDateTime? = null): LocalDateTime {
        return _getSession(region ?: _defaultLocation, assetClass ?: _defaultAsset).getDayStart(date).getDateTime(date)
    }

    fun getDayEnd(region: Region? = null, assetClass: AssetClass? = null, date: LocalDateTime? = null): LocalDateTime {
        val session = _getSession(region ?: _defaultLocation, assetClass ?: _defaultAsset)
        return session.getDayEnd(date).getDateTime(date)
    }

    fun getPreviousTradingDay(region: Region? = null, assetClass: AssetClass? = null, date: LocalDateTime? = null): LocalDateTime {
        val session = _getSession(region ?: _defaultLocation, assetClass ?: _defaultAsset)
        return session.getPreviousClosestActiveDate(date)
    }

    fun getNextTradingDay(region: Region? = null, assetClass: AssetClass? = null, date: LocalDateTime? = null): LocalDateTime {
        val session = _getSession(region ?: _defaultLocation, assetClass ?: _defaultAsset)
        return session.getNextClosestActiveDate(date)
    }

    fun isDuringMarketHours(region: Region? = null, assetClass: AssetClass? = null, date: LocalDateTime? = null): Boolean {
        val session = _getSession(region ?: _defaultLocation, assetClass ?: _defaultAsset)
        return session.isDuringMarketHours(date)
    }
    fun getPeriodStart(period: StockDataPeriods, date: LocalDateTime? = null, region: Region? = null, assetClass: AssetClass? = null): LocalDateTime {
        lateinit var diff: Duration
        when (period) {
            StockDataPeriods.Price1D -> return _getSession(region ?: _defaultLocation, assetClass ?: _defaultAsset).getDayStart(date).getDateTime(date)
            StockDataPeriods.Price1W -> diff = Duration.ofDays(7)
            StockDataPeriods.Price1M -> diff = Duration.ofDays(30)
            StockDataPeriods.Price3M -> diff = Duration.ofDays(91)
            StockDataPeriods.Price1Y -> diff = Duration.ofDays(365)
            StockDataPeriods.Price5Y -> diff = Duration.ofDays(365 * 5)
            StockDataPeriods.PriceAllTime -> diff = Duration.ofDays(365 * 100)
        }

        return _getSession(region ?: _defaultLocation, assetClass ?: _defaultAsset).getNextClosestActiveDate(date = date ?: LocalDateTime.now().minus(diff))
    }

    fun getFinancialQuarterText(date: LocalDateTime, separator: String = "/", completeYear: Boolean = false): String {
        val quarter = FinancialQuarter.fromDate(date)
        return "${if (completeYear) quarter.year.toString() else quarter.year.toString().substring(2)}$separator${quarter.month.toString()}"
    }


    suspend fun init() {
        while (!_initialized) {
            try {
                val sessions = sessionsRepo.getData(null) ?: throw Exception("Sessions is null")
                for (session in sessions) {
                    _sessions[session.assetClass]?.let {
                        it[session.region] = session
                    } ?: run {
                        _sessions[session.assetClass] = mutableMapOf(session.region to session)
                    }                }
            } catch (e: Exception) {
                delay(500L)
            }
        }
    }
}
