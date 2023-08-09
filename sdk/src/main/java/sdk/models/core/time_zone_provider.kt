package sdk.models.core

import java.time.Duration
import java.time.Instant

class LocalDateTime {
    companion object{
        fun now(): Instant {
            return Instant.now()
        }
        fun delayed(): Instant {
            return now().minusSeconds(15 * 60)
        }
    }
}