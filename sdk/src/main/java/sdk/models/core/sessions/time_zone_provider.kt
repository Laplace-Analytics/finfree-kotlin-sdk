package sdk.models.core.sessions

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset


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
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), ZoneOffset.UTC)
        }
    }
}
