package sdk.repositories

import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.zone.ZoneRulesProvider
import sdk.api.CoreApiProvider
import sdk.base.GenericRepository
import sdk.base.GenericStorage
import sdk.base.logger
import sdk.base.network.BasicResponseTypes
import sdk.models.*
import sdk.models.core.ClosePoint
import sdk.models.core.HourMinuteSecond
import sdk.models.core.OpenPoint
import sdk.models.core.Sessions
import java.time.LocalDateTime

typealias GetLocalTimezone = suspend () -> String


open class SessionsRepo(
    storageHandler: GenericStorage,
    apiProvider: CoreApiProvider,
    private val getLocalTimezone: GetLocalTimezone
) : GenericRepository<List<Sessions>, SessionsRepoIdentifier, CoreApiProvider>(storageHandler, apiProvider) {

    override suspend fun fetchData(identifier: SessionsRepoIdentifier?): List<Sessions>? {
        val region = identifier?.region
        val assetClass = identifier?.assetClass
        try {
            val response = apiProvider.getSessions(
                region = region?.string(),
                assetClass = assetClass?.string()
            )

            if (response.responseType != BasicResponseTypes.Success || response.data == null) {
                return null
            }

            ZoneRulesProvider.getAvailableZoneIds()//control
            ZoneId.systemDefault()//uygulma baslatildigina AnroidThreeTen.init(this) cagirilmali!!

            val sessions = processRawData(mapOf("data" to response.data))
            return sessions
        } catch (ex: Exception) {
            logger.error("error occurred trying to get all stocks", ex)
            return null
        }
    }

    fun processRawData(rawData: Map<String, Any?>): List<Sessions>? {
        val sessions = mutableListOf<Sessions>()
        val data = rawData["data"] as List<Map<String, Any?>>?

        data?.forEach { json ->
            val timeZone = json["time_zone"] as String?
            val daysOpen = json["days_open"] as List<Int>?
            val timeOpen = json["time_open"] as String?
            val timeClose = json["time_close"] as String?

            if (timeZone == null || daysOpen == null || timeOpen == null || timeClose == null) {
                return@forEach
            }

            val zoneId = ZoneId.of(timeZone)
            val now = ZonedDateTime.now(zoneId)
            val openHour = timeOpen.substring(0, 2).toIntOrNull()
            val openMinute = timeOpen.substring(3).toIntOrNull()
            if (openHour == null || openMinute == null) {
                return@forEach
            }

            val openDate = LocalDateTime.of(now.year, now.monthValue, now.dayOfMonth, openHour, openMinute)
                .minusDays((now.dayOfWeek.value - 1).toLong())

            val closeHour = timeClose.substring(0, 2).toIntOrNull()
            val closeMinute = timeClose.substring(3).toIntOrNull()
            if (closeHour == null || closeMinute == null) {
                return@forEach
            }

            val closeDate = LocalDateTime.of(now.year, now.monthValue, now.dayOfMonth, closeHour, closeMinute)
                .minusDays((now.dayOfWeek.value - 1).toLong())

            val openDateConverted = openDate
            val closeDateConverted = closeDate

            val weekDayToSession = mutableListOf<HourMinuteSecond>()

            daysOpen.forEach { day ->
                weekDayToSession.add(OpenPoint.fromDateTime(openDateConverted.plusDays(day.toLong())))
                weekDayToSession.add(ClosePoint.fromDateTime(closeDateConverted.plusDays(day.toLong())))
            }

            sessions.add(
                Sessions(
                    assetClass = (json["asset_class"] as String).assetClass(),
                    region = (json["region"] as String).region(),
                    points = weekDayToSession
                )
            )
        }

        return sessions
    }


    override fun getFromJson(json: Map<String, Any>): List<Sessions> {
        val data = json["data"] as List<Map<String, Any>>
        return data.map { e -> Sessions.fromJson(e) }
    }

    override fun getIdentifier(data: List<Sessions>): SessionsRepoIdentifier? {
        throw NotImplementedError("Not implemented yet")
    }

    override fun getPath(identifier: SessionsRepoIdentifier?): String {
        throw NotImplementedError("Not implemented yet")
    }

    override fun toJson(data: List<Sessions>): Map<String, Any> {
        return mapOf("data" to data.map { it.toJson() })
    }
}

data class SessionsRepoIdentifier(
    val region:Region? = null,
    val assetClass: AssetClass? = null
)
