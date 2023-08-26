package sdk.models.core.sessions

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime


class DateTime {
    companion object {
        fun now(): LocalDateTime {
            return LocalDateTime.now()
        }
        fun delayed(): LocalDateTime {
            return LocalDateTime.now().minusSeconds(15 * 60)
        }
        fun LocalDateTime.toEpochMilliSecond(): Long {
            return this.toEpochSecond(ZoneOffset.UTC) * 1000
        }

        fun fromSinceEpochMilliSecond(epochMilli: Long): LocalDateTime {
            val instant = Instant.ofEpochMilli(epochMilli)
            val zonedDateTimeUTC = ZonedDateTime.ofInstant(instant, ZoneId.of("UTC"))

            val zonedDateTimeGMTPlus3 = zonedDateTimeUTC.withZoneSameInstant(ZoneId.of("GMT+3"))

            return zonedDateTimeGMTPlus3.toLocalDateTime()
        }
    }
}
