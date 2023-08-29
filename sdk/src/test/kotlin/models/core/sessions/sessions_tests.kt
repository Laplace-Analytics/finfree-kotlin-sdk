package models.core.sessions

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import sdk.models.AssetClass
import sdk.models.Region
import sdk.models.core.HourMinuteSecond
import sdk.models.core.Sessions
import sdk.models.core.getDateTime
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.util.Random

class SessionTests{
    val sessions = sessionsString.map { Sessions.fromJson(it) }

    val trEquity = sessions.first { it.assetClass == AssetClass.equity && it.region == Region.turkish }
    val trCrypto = sessions.first { it.assetClass == AssetClass.crypto && it.region == Region.turkish }
    val usEquity = sessions.first { it.assetClass == AssetClass.equity && it.region == Region.american }

    @Test
    fun `weekend previous closest active date`() {
        val date = LocalDateTime.of(2022, 3, 13, 15, 0)
        val previousActive = trEquity.getPreviousClosestActiveDate(date = date)
        assertEquals(LocalDateTime.of(2022, 3, 11, 18, 0), previousActive)
    }

    @Test
    fun `weekend next closest active date`() {
        val date = LocalDateTime.of(2022, 3, 12, 15, 0)
        val previousActive = trEquity.getNextClosestActiveDate(date = date)
        assertEquals(LocalDateTime.of(2022, 3, 14, 10, 0), previousActive)
    }

    @Test
    fun `weekend day end`() {
        val date = LocalDateTime.of(2022, 3, 12, 15, 0)
        val previousActive = trEquity.getDayEnd(date = date).getDateTime(date = date)
        assertEquals(LocalDateTime.of(2022, 3, 11, 18, 0), previousActive)
    }

    @Test
    fun `weekend day start`() {
        val date = LocalDateTime.of(2022, 3, 12, 15, 0)
        val previousActive = trEquity.getDayStart(date = date).getDateTime(date = date)
        assertEquals(LocalDateTime.of(2022, 3, 11, 10, 0), previousActive)
    }
    @Nested
    inner class CDateBasicUnitTests{
        val cDate = HourMinuteSecond(hour = 18, minute = 30, second = 0)
        val testDate = LocalDateTime.of(2022, 1, 10, 15, 30, 0)

        @Test
        fun `isBefore`() {
            val result = cDate.isBefore(dateTime = testDate)
            assertEquals(false, result)
        }

        @Test
        fun `isAfter`() {
            val result = cDate.isAfter(dateTime = testDate)
            assertEquals(true, result)
        }

        @Test
        fun `isEqual false`() {
            val result = cDate.isEqual(dateTime = testDate)
            assertEquals(false, result)
        }

        @Test
        fun `isEqual true`() {
            val result = cDate.isEqual(date = cDate)
            assertEquals(true, result)
        }

        @Test
        fun `isAfter with same date`() {
            val result = cDate.isAfter(date = cDate)
            assertEquals(false, result)
        }

        @Test
        fun `isBefore with same date`() {
            val result = cDate.isBefore(hourMinuteSecond = cDate)
            assertEquals(false, result)
        }

        @Test
        fun `cDate constructor with DateTime`() {
            val newCDate = HourMinuteSecond.fromDateTime(testDate)
            val result = newCDate.hour == testDate.hour && newCDate.minute == testDate.minute && newCDate.second == testDate.second
            assertEquals(true, result)
        }
    }
    @Nested
    inner class SessionBasicUnitTests{
        @Test
        fun `getDateTime extension`() {
            //zamanda sıkıntı var
            val cDate = HourMinuteSecond(hour = 18, minute = 30, second = 0)
            val date = cDate.getDateTime()
            val now = LocalDateTime.of(2023, 8, 28, 18, 30, 0)
            assertEquals(LocalDateTime.of(now.year, now.monthValue, now.dayOfMonth, cDate.hour, cDate.minute, cDate.second), date)
        }

        @Test
        fun `getDate`() {
            assertEquals(getDate(DateStatus.InDay, 30, Region.turkish), LocalDateTime.of(2021, 12, 30, 12, 0, 0))
            assertEquals(getDate(DateStatus.AfterDay, 30, Region.turkish), LocalDateTime.of(2021, 12, 30, 21, 0, 0))
            assertEquals(getDate(DateStatus.BeforeDay, 30, Region.turkish), LocalDateTime.of(2021, 12, 30, 8, 0, 0))
            assertEquals(getDate(DateStatus.AfterDay, 30, Region.turkish, month = 10, year = 2020), LocalDateTime.of(2020, 10, 30, 21, 0, 0))
            assertEquals(getDate(DateStatus.InDay, 30, Region.american), LocalDateTime.of(2021, 12, 30, 21, 0, 0))
            assertEquals(getDate(DateStatus.AfterDay, 30, Region.american), LocalDateTime.of(2021, 12, 30, 15, 30, 0))
            assertEquals(getDate(DateStatus.BeforeDay, 30, Region.american), LocalDateTime.of(2021, 12, 30, 15, 0, 0))
            assertEquals(getDate(DateStatus.BeforeDay, 30, Region.american, month = 10, year = 2020), LocalDateTime.of(2020, 10, 30, 15, 0, 0))
        }

    }
    @Test
    fun `TR Stock Session 2`() {
        sessionsTest(trEquity)
    }

    @Test
    fun `US Stock Session 2`() {
        sessionsTest(usEquity)
    }

    @Test
    fun `Crypto Session 2`() {
        sessionsTest(trCrypto)
    }
}

fun sessionsTest(sessions: Sessions) {
    val randomDate = randomDateInRange(2010, 2030)
    val randomGenerator = Random()

    val dateGetter = DateGetter.create(sessions.assetClass, sessions.region)

    for (i in 0 until 100) {
        val date = LocalDateTime.of(randomDate.year, randomDate.monthValue, randomDate.dayOfMonth, randomGenerator.nextInt(23), randomGenerator.nextInt(59))
        val comp = compareToMarketHours(date, sessions.region)
        when (date.dayOfWeek) {
            DayOfWeek.MONDAY -> {
                when (comp) {
                    -1 -> {
                        assertEquals(
                            sessions.getNextClosestActiveDate(date),
                            dateGetter.dayStart(date)
                        )
                        assertEquals(
                            sessions.getPreviousClosestActiveDate(date),
                            dateGetter.dayEndWithDifference(-3, date)
                        )
                    }
                    1 -> {
                        assertEquals(
                            sessions.getNextClosestActiveDate(date),
                            dateGetter.dayStartWithDifference(1, date)
                        )
                        assertEquals(
                            sessions.getPreviousClosestActiveDate(date),
                            dateGetter.dayEnd(date)
                        )
                    }
                    0 -> {
                        assertEquals(sessions.getNextClosestActiveDate(date), date)
                        assertEquals(sessions.getPreviousClosestActiveDate(date), date)
                    }
                }
            }
            DayOfWeek.FRIDAY -> {
                when (comp) {
                    -1 -> {
                        assertEquals(
                            sessions.getNextClosestActiveDate(date),
                            dateGetter.dayStart(date)
                        )
                        assertEquals(
                            sessions.getPreviousClosestActiveDate(date),
                            dateGetter.dayEndWithDifference(-1, date)
                        )
                    }
                    1 -> {
                        assertEquals(
                            sessions.getNextClosestActiveDate(date),
                            dateGetter.dayStartWithDifference(3, date)
                        )
                        assertEquals(
                            sessions.getPreviousClosestActiveDate(date),
                            dateGetter.dayEnd(date)
                        )
                    }
                    0 -> {
                        assertEquals(sessions.getNextClosestActiveDate(date), date)
                        assertEquals(sessions.getPreviousClosestActiveDate(date), date)
                    }
                }
            }
            DayOfWeek.SATURDAY -> {
                assertEquals(
                    sessions.getNextClosestActiveDate(date),
                    if (sessions.assetClass == AssetClass.crypto) date else dateGetter.dayStartWithDifference(2, date)
                )
                assertEquals(
                    sessions.getPreviousClosestActiveDate(date),
                    if (sessions.assetClass == AssetClass.crypto) date else dateGetter.dayEndWithDifference(-1, date)
                )
            }
            DayOfWeek.SUNDAY -> {
                assertEquals(
                    sessions.getNextClosestActiveDate(date),
                    if (sessions.assetClass == AssetClass.crypto) date else dateGetter.dayStartWithDifference(1, date)
                )
                assertEquals(
                    sessions.getPreviousClosestActiveDate(date),
                    if (sessions.assetClass == AssetClass.crypto) date else dateGetter.dayEndWithDifference(-2, date)
                )
            }
            else -> {
                when (comp) {
                    -1 -> {
                        assertEquals(
                            sessions.getNextClosestActiveDate(date),
                            dateGetter.dayStart(date)
                        )
                        assertEquals(
                            sessions.getPreviousClosestActiveDate(date),
                            dateGetter.dayEndWithDifference(-1, date)
                        )
                    }
                    1 -> {
                        assertEquals(
                            sessions.getNextClosestActiveDate(date),
                            dateGetter.dayStartWithDifference(1, date)
                        )
                        assertEquals(
                            sessions.getPreviousClosestActiveDate(date),
                            dateGetter.dayEnd(date)
                        )
                    }
                    0 -> {
                        assertEquals(sessions.getNextClosestActiveDate(date), date)
                        assertEquals(sessions.getPreviousClosestActiveDate(date), date)
                    }
                }
            }
        }
    }
}

fun randomDateInRange(startYear: Int, endYear: Int): LocalDateTime {
    val random = Random()
    val year = random.nextInt(endYear - startYear + 1) + startYear
    val month = random.nextInt(12) + 1
    val dayOfMonth = random.nextInt(28)  + 1 // En basit haliyle her ayı 28 gün olarak aldık.
    return LocalDateTime.of(year, month, dayOfMonth,0,0)
}
