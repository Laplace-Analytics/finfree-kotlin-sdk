package base

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import sdk.api.StockDataPeriods
import sdk.base.FinancialFormat
import sdk.base.formatDouble
import sdk.base.formatFinancialValue
import sdk.base.formatLeaguePrize
import sdk.base.formatPriceDouble
import sdk.base.getAllTimePeriod
import sdk.base.getDoubleFromDynamic
import sdk.base.getOwnedStockCountText
import sdk.base.getTimePeriod
import sdk.base.withEnglishCharacters
import sdk.models.data.assets.Currency
import java.time.LocalDateTime


const val spaceCharacter:String = " "

class HelperFunctionsTest{
    val testSingleDigitNumberList = listOf(1.3434, 0.0, 4.0, 6.8000430)
    val testDoubleDigitNumberList = listOf(11.3434, 20.0, 43.0, 61.8000430)
    val testTripleDigitNumberList = listOf(111.3434, 320.0, 423.0, 261.8000430)
    val testNegativeDoubleDigitNumberList = listOf(-11.3434, -32.0, -42.00000001, -12.09)
    val testNegativeSingleDigitNumberList = listOf(-0.3434, -3.0, -4.00000001, -2.09)
    val testDynamicTypeNumberList = listOf("12.34", 233, 54.99, "0", 0, -5, "-67")


    val financialFormatJsonList = listOf(
        mapOf(
            "key" to "ev-to-ebitda",
            "name" to "FD/FAVÖK",
            "description" to "",
            "suffix" to "",
            "prefix" to "",
            "multiplier" to 1.0,
            "precision" to 1,
            "display" to true,
            "interval" to "trailing"
        ),
        mapOf(
            "key" to "satis_buyumesi",
            "name" to "Satış Büyümesi",
            "description" to "",
            "suffix" to "",
            "prefix" to "%",
            "multiplier" to 100.0,
            "precision" to 1,
            "display" to true,
            "interval" to "trailing,"
        ),
        mapOf(
            "key" to "net_borc_favok",
            "name" to "Net Borç/FAVÖK",
            "description" to "",
            "suffix" to "",
            "prefix" to "",
            "multiplier" to 1.0,
            "precision" to 1,
            "display" to true,
            "interval" to "trailing"
        ),
        mapOf(
            "key" to "net-margin",
            "name" to "Net Kâr Marjı",
            "description" to "",
            "suffix" to "",
            "prefix" to "%",
            "multiplier" to 100.0,
            "precision" to 1,
            "display" to true,
            "interval" to "quarterly"
        ),
        mapOf(
            "key" to "rsi",
            "name" to "RSI - Göreceli Güç İndikatörü",
            "description" to "",
            "suffix" to "",
            "prefix" to "%",
            "multiplier" to 1.0,
            "precision" to 0,
            "display" to true,
            "interval" to "live"
        )
    )

    val financialKeyValueMapList = listOf(
        mapOf("ev-to-ebitda" to 31.759001756189246),
        mapOf("satis_buyumesi" to 0.7268740515671805),
        mapOf("net_borc_favok" to null),
        mapOf("net-margin" to 0.21644146109019854),
        mapOf("rsi" to 90.37294934941085)
    )

    @Nested
    inner class FormatDoubleTests{
        @Test
        fun basicKnownUsageTest() {
            val formattedDouble = formatDouble(256.0, 4)
            assertEquals("256,0000", formattedDouble.trim())

            val formattedDouble2 = formatDouble(null, 4, "₺")
            assertEquals("-", formattedDouble2.trim())

            val formattedDouble3 = formatDouble(0.0)
            assertEquals("0,00", formattedDouble3.trim())

            val formattedDouble4 = formatDouble(-12.23, 2, "₺")
            assertEquals("−12,23${spaceCharacter}₺", formattedDouble4.trim())

            val formattedDouble5 = formatDouble(9.091, 4, suffix = "BTC")
            assertEquals("9,0910${spaceCharacter}BTC", formattedDouble5)

            val formattedDouble6 = formatDouble(16.0003, 3, suffix = "BTC", adjustPrecision = true)
            assertEquals("16,000${spaceCharacter}BTC", formattedDouble6)


            val formattedDouble7 = formatDouble(160.0003, 3, adjustPercentagePrecision = true)
            assertEquals("160", formattedDouble7.trim())

            val formattedDouble8 = formatDouble(84.23423523, adjustPrecision = true, adjustPercentagePrecision = true)
            assertEquals("84,23", formattedDouble8.trim())

            val formattedDouble9 = formatDouble(84.23423523, adjustPercentagePrecision = true)
            assertEquals("84,2", formattedDouble9.trim())
        }
    }
    @Nested
    inner class PercentageUsageTests{
        var currentTestNumberLists: List<List<Double>> = listOf(
            testSingleDigitNumberList,
            testTripleDigitNumberList,
            testNegativeDoubleDigitNumberList,
            testNegativeSingleDigitNumberList
        )

        @Test
        fun `Percentage usage test with constant adjustPercentagePrecision true, prefix %`() {
            var digitNumberBeforeComma: Int? = null
            var digitNumberAfterComma: Int? = null
            var digitNumberEntireText: Int? = null

            for (testNumberList in currentTestNumberLists) {
                for (formatValue in testNumberList) {
                    val formattedValue = formatDouble(formatValue, adjustPercentagePrecision = true, prefix = "%").trim()
                    assertEquals('%', formattedValue[0])
                    val splittedValue = formattedValue.split(",")

                    val tempDigitNumberBeforeComma = splittedValue[0].length
                    val tempDigitNumberAfterComma = if (splittedValue.size > 1) splittedValue[1].length else 0
                    val tempDigitNumberEntireText = formattedValue.length

                    if (digitNumberBeforeComma == null && digitNumberAfterComma == null && digitNumberEntireText == null) {
                        digitNumberBeforeComma = tempDigitNumberBeforeComma
                        digitNumberAfterComma = tempDigitNumberAfterComma
                        digitNumberEntireText = tempDigitNumberEntireText
                    } else {
                        assertEquals(digitNumberBeforeComma, tempDigitNumberBeforeComma)
                        assertEquals(digitNumberAfterComma, tempDigitNumberAfterComma)
                        assertTrue(digitNumberAfterComma!! <= 2)
                        assertEquals(digitNumberEntireText, tempDigitNumberEntireText)

                        if (digitNumberBeforeComma != tempDigitNumberBeforeComma ||
                            digitNumberAfterComma != tempDigitNumberAfterComma ||
                            digitNumberEntireText != tempDigitNumberEntireText
                        ) {
                            fail("Digit numbers do not match with same digit number double values in formatDouble function")
                        } else {
                            digitNumberBeforeComma = tempDigitNumberBeforeComma
                            digitNumberAfterComma = tempDigitNumberAfterComma
                            digitNumberEntireText = tempDigitNumberEntireText
                        }
                    }
                }
                digitNumberEntireText = null
                digitNumberAfterComma = null
                digitNumberBeforeComma = null
            }
        }

        @Test
        fun `Percentage usage test with constant precision 3, adjustPercentagePrecision false, prefix %`() {
            var digitNumberBeforeComma: Int? = null
            var digitNumberAfterComma: Int? = null
            var digitNumberEntireText: Int? = null

            for (testNumberList in currentTestNumberLists) {
                for (formatValue in testNumberList) {
                    val formattedValue = formatDouble(
                        formatValue,
                        precision = 3,
                        adjustPercentagePrecision = false,
                        prefix = "%"
                    ).trim()
                    assertEquals('%', formattedValue[0])
                    val splittedValue = formattedValue.split(",")

                    val tempDigitNumberBeforeComma = splittedValue[0].length
                    val tempDigitNumberAfterComma = if (splittedValue.size > 1) splittedValue[1].length else 0
                    val tempDigitNumberEntireText = formattedValue.length

                    if (digitNumberBeforeComma == null && digitNumberAfterComma == null && digitNumberEntireText == null) {
                        digitNumberBeforeComma = tempDigitNumberBeforeComma
                        digitNumberAfterComma = tempDigitNumberAfterComma
                        digitNumberEntireText = tempDigitNumberEntireText
                    } else {
                        assertEquals(digitNumberBeforeComma, tempDigitNumberBeforeComma)
                        assertEquals(3, digitNumberAfterComma)
                        assertEquals(digitNumberAfterComma, tempDigitNumberAfterComma)
                        assertEquals(digitNumberEntireText, tempDigitNumberEntireText)

                        if (digitNumberBeforeComma != tempDigitNumberBeforeComma ||
                            digitNumberAfterComma != tempDigitNumberAfterComma ||
                            digitNumberEntireText != tempDigitNumberEntireText
                        ) {
                            fail("Digit numbers do not match with same digit number double values in formatDouble function")
                        } else {
                            digitNumberBeforeComma = tempDigitNumberBeforeComma
                            digitNumberAfterComma = tempDigitNumberAfterComma
                            digitNumberEntireText = tempDigitNumberEntireText
                        }
                    }
                }
                digitNumberEntireText = null
                digitNumberAfterComma = null
                digitNumberBeforeComma = null
            }
        }
    }
    @Nested
    inner class PriceUsageTests{
        val currentTestNumberLists = listOf(testSingleDigitNumberList, testDoubleDigitNumberList, testTripleDigitNumberList)

        @Test
        fun `Price usage test with constant adjustPrecision true, suffix ₺`() {
            var digitNumberBeforeComma: Int? = null
            var digitNumberAfterComma: Int? = null
            var digitNumberEntireText: Int? = null

            for (testNumberList in currentTestNumberLists) {
                for (formatValue in testNumberList) {
                    val formattedValue = formatDouble(formatValue, adjustPrecision = true, suffix = "₺").trim()
                    assertEquals('₺', formattedValue.last())
                    val splittedValue = formattedValue.split(",")

                    val tempDigitNumberBeforeComma = splittedValue[0].length
                    val tempDigitNumberAfterComma = if (splittedValue.size > 1) splittedValue[1].length else 0
                    val tempDigitNumberEntireText = formattedValue.length

                    if (digitNumberBeforeComma == null && digitNumberAfterComma == null && digitNumberEntireText == null) {
                        digitNumberBeforeComma = tempDigitNumberBeforeComma
                        digitNumberAfterComma = tempDigitNumberAfterComma
                        digitNumberEntireText = tempDigitNumberEntireText
                    } else {
                        assertEquals(digitNumberBeforeComma, tempDigitNumberBeforeComma)
                        assertEquals(digitNumberAfterComma, tempDigitNumberAfterComma)
                        assertEquals(digitNumberEntireText, tempDigitNumberEntireText)

                        if (digitNumberBeforeComma != tempDigitNumberBeforeComma ||
                            digitNumberAfterComma != tempDigitNumberAfterComma ||
                            digitNumberEntireText != tempDigitNumberEntireText
                        ) {
                            fail("Digit numbers do not match with same digit number double values in formatDouble function")
                        } else {
                            digitNumberBeforeComma = tempDigitNumberBeforeComma
                            digitNumberAfterComma = tempDigitNumberAfterComma
                            digitNumberEntireText = tempDigitNumberEntireText
                        }
                    }
                }
                digitNumberEntireText = null
                digitNumberAfterComma = null
                digitNumberBeforeComma = null
            }
        }
        @Test
        fun `Price usage test with constant currency dollarSign`() {
            var digitNumberBeforeComma: Int? = null
            var digitNumberAfterComma: Int? = null
            var digitNumberEntireText: Int? = null

            for (testNumberList in currentTestNumberLists) {
                for (formatValue in testNumberList) {
                    val formattedValue = formatDouble(formatValue, currency = "USD").trim()
                    assertEquals('$', formattedValue.last())
                    val splittedValue = formattedValue.split(",")

                    val tempDigitNumberBeforeComma = splittedValue[0].length
                    val tempDigitNumberAfterComma = if (splittedValue.size > 1) splittedValue[1].length else 0
                    val tempDigitNumberEntireText = formattedValue.length

                    if (digitNumberBeforeComma == null && digitNumberAfterComma == null && digitNumberEntireText == null) {
                        digitNumberBeforeComma = tempDigitNumberBeforeComma
                        digitNumberAfterComma = tempDigitNumberAfterComma
                        digitNumberEntireText = tempDigitNumberEntireText
                    } else {
                        assertEquals(digitNumberBeforeComma, tempDigitNumberBeforeComma)
                        assertEquals(digitNumberAfterComma, tempDigitNumberAfterComma)
                        assertEquals(digitNumberEntireText, tempDigitNumberEntireText)

                        if (digitNumberBeforeComma != tempDigitNumberBeforeComma ||
                            digitNumberAfterComma != tempDigitNumberAfterComma ||
                            digitNumberEntireText != tempDigitNumberEntireText
                        ) {
                            fail("Digit numbers do not match with same digit number double values in formatDouble function")
                        } else {
                            digitNumberBeforeComma = tempDigitNumberBeforeComma
                            digitNumberAfterComma = tempDigitNumberAfterComma
                            digitNumberEntireText = tempDigitNumberEntireText
                        }
                    }
                }
                digitNumberEntireText = null
                digitNumberAfterComma = null
                digitNumberBeforeComma = null
            }
        }
    }
    @Nested
    inner class FormatPriceDoubleTests{
        @Test
        fun `Basic known usage test`() {
            val formattedDouble = formatPriceDouble(12753.34623453, adjustPrecision = true, precision = 3, currency = Currency.Tl)

            val formattedDouble6 = formatPriceDouble(12753.346, currency = null)
            assertEquals("12.753", formattedDouble6.trim())

            val formattedDouble2 = formatPriceDouble(-23.5, currency = Currency.Usd)


            val formattedDouble4 = formatPriceDouble(null, currency = Currency.Usd)
            assertEquals("-", formattedDouble4.trim())

            val formattedDouble5 = formatPriceDouble(0.0, currency = null)
            assertEquals("0,00", formattedDouble5.trim())

            val formattedDouble7 = formatPriceDouble(14324.23423, currency = Currency.Tl, suffix = "\$")

            val formattedDouble8 = formatPriceDouble(6232343.43, currency = Currency.Tl, prefix = "~")

            val formattedDouble9 = formatPriceDouble(4674.82, adjustPrecision = false, precision = 4, currency = Currency.Tl)

            val formattedDouble10 = formatPriceDouble(-0.82, precision = 4, currency = Currency.Tl)
        }

    }
    @Nested
    inner class GetDoubleFromDynamicTests{
        @Test
        fun `Basic known usage test`() {
            val formattedDouble = getDoubleFromDynamic(12)
            assertEquals(12.0, formattedDouble)

            val formattedDouble2 = getDoubleFromDynamic(null)
            assertNull(formattedDouble2)

            val formattedDouble3 = getDoubleFromDynamic("45,4")
            assertNull(formattedDouble3)

            val formattedDouble4 = getDoubleFromDynamic("45.4")
            assertEquals(45.4, formattedDouble4)

            val formattedDouble5 = getDoubleFromDynamic(45.4)
            assertEquals(45.4, formattedDouble5)

            val formattedDouble6 = getDoubleFromDynamic("cdv")
            assertNull(formattedDouble6)
        }

        @Test
        fun `Return type test`() {
            for (number in testDynamicTypeNumberList) {
                val formattedDouble = getDoubleFromDynamic(number)
                if (formattedDouble !is Double) {
                    fail("getDoubleFromDynamic returns a different type from double")
                }
            }
        }
    }
    @Nested
    inner class GetOwnedStockCountTextTests{
        @Test
        fun `Basic known usage test`() {
            val ownedStockCountText = getOwnedStockCountText(3.0)
            assertEquals("3", ownedStockCountText)

            val ownedStockCountText2 = getOwnedStockCountText(12.4)
            println(ownedStockCountText2)
            assertEquals("12,4", ownedStockCountText2.trim())

            val ownedStockCountText3 = getOwnedStockCountText(70.347568437)
            assertEquals("70,348", ownedStockCountText3.trim())

            val ownedStockCountText4 = getOwnedStockCountText(301.347568437)
            assertEquals("301,348", ownedStockCountText4.trim())

            val ownedStockCountText5 = getOwnedStockCountText(1.9)
            assertEquals("1,9", ownedStockCountText5.trim())

            val ownedStockCountText6 = getOwnedStockCountText(1.999999)
            assertEquals("2", ownedStockCountText6.trim())

            val ownedStockCountText7 = getOwnedStockCountText(457457.0)
            assertEquals("457457", ownedStockCountText7.trim())

            val ownedStockCountText8 = getOwnedStockCountText(0.1)
            assertEquals("0,1", ownedStockCountText8.trim())

            val ownedStockCountText9 = getOwnedStockCountText(1.000001)
            assertEquals("1", ownedStockCountText9.trim())

            val ownedStockCountText10 = getOwnedStockCountText(1.00001)
            assertEquals("1,000", ownedStockCountText10.trim())
        }
    }
    @Nested
    inner class FormatLeaguePrizeTests{
        @Test
        fun `Basic known usage test`() {
            val formattedLeaguePrizeText = formatLeaguePrize(10000.0)
            assertEquals("10,00b ₺", formattedLeaguePrizeText)

            val formattedLeaguePrizeText2 = formatLeaguePrize(10500.0)
            assertEquals("10,50b ₺", formattedLeaguePrizeText2)

            val formattedLeaguePrizeText3 = formatLeaguePrize(19999.0)
            assertEquals("20b ₺", formattedLeaguePrizeText3)

            val formattedLeaguePrizeText4 = formatLeaguePrize(550000.0)
            assertEquals("550,00b ₺", formattedLeaguePrizeText4)

            val formattedLeaguePrizeText5 = formatLeaguePrize(0.0)
            assertEquals("0₺", formattedLeaguePrizeText5)
        }
    }

    @Nested
    inner class TurkishToEnglishCharsTests{
        @Test
        fun basicKnownUsageTest() {
            val formattedString = "str".withEnglishCharacters()
            assertEquals("str", formattedString)

            val formattedString2 = "Eczacıbaşı Yatırım".withEnglishCharacters()
            assertEquals("Eczacibasi Yatirim", formattedString2)

            val formattedString3 = "Doğuş Otomotiv Servis".withEnglishCharacters()
            assertEquals("Dogus Otomotiv Servis", formattedString3)

            val formattedString4 = "Çarşamba".withEnglishCharacters()
            assertEquals("Carsamba", formattedString4)
        }
    }
    @Nested
    inner class GetAllTimePeriodTests{
        @Test
        fun basicKnownUsageTest() {
            val stockDataPeriod = getAllTimePeriod(LocalDateTime.of(2020, 3, 30,0,0), LocalDateTime.of(2000, 3, 30,0,0))
            assertEquals(StockDataPeriods.Price5Y, stockDataPeriod)

            val stockDataPeriod2 = getAllTimePeriod(LocalDateTime.of(2020, 4, 30,0,0), LocalDateTime.of(2020, 1, 30,0,0))
            assertEquals(StockDataPeriods.Price3M, stockDataPeriod2)

            val stockDataPeriod3 = getAllTimePeriod(LocalDateTime.of(2020, 5, 27,0,0), LocalDateTime.of(2020, 4, 30,0,0))
            assertEquals(StockDataPeriods.Price1M, stockDataPeriod3)

            val stockDataPeriod4 = getAllTimePeriod(LocalDateTime.of(2020, 5, 27,0,0), LocalDateTime.of(2019, 2, 28,0,0))
            assertEquals(StockDataPeriods.Price1Y, stockDataPeriod4)

            val stockDataPeriod5 = getAllTimePeriod(LocalDateTime.of(2020, 5, 27,0,0), LocalDateTime.of(2020, 5, 16,0,0))
            assertEquals(StockDataPeriods.Price1W, stockDataPeriod5)

            val stockDataPeriod6 = getAllTimePeriod(LocalDateTime.of(2020, 5, 27,0,0), LocalDateTime.of(2020, 5, 13,0,0))
            assertEquals(StockDataPeriods.Price1M, stockDataPeriod6)
        }
    }
    @Nested
    inner class GetTimePeriodTests{
        @Test
        fun `Basic known usage test`() {
            val stockDataPeriod = getTimePeriod(LocalDateTime.of(2020, 3, 30, 0, 0), LocalDateTime.of(2000, 3, 30, 0, 0))
            assertEquals(StockDataPeriods.Price5Y, stockDataPeriod)

            val stockDataPeriod2 = getTimePeriod(LocalDateTime.of(2020, 4, 30, 0, 0), LocalDateTime.of(2020, 1, 30, 0, 0))
            assertEquals(StockDataPeriods.Price1Y, stockDataPeriod2)

            val stockDataPeriod3 = getTimePeriod(LocalDateTime.of(2020, 5, 27, 0, 0), LocalDateTime.of(2020, 4, 30, 0, 0))
            assertEquals(StockDataPeriods.Price1M, stockDataPeriod3)

            val stockDataPeriod4 = getTimePeriod(LocalDateTime.of(2020, 5, 27, 0, 0), LocalDateTime.of(2019, 2, 28, 0, 0))
            assertEquals(StockDataPeriods.Price5Y, stockDataPeriod4)

            val stockDataPeriod5 = getTimePeriod(LocalDateTime.of(2020, 5, 27, 0, 0), LocalDateTime.of(2020, 5, 16, 0, 0))
            assertEquals(StockDataPeriods.Price1M, stockDataPeriod5)

            val stockDataPeriod6 = getTimePeriod(LocalDateTime.of(2020, 5, 27, 0, 0), LocalDateTime.of(2020, 5, 13, 0, 0))
            assertEquals(StockDataPeriods.Price1M, stockDataPeriod6)
        }
    }
    @Nested
    inner class FormatFinancialValueTest{
        @Test
        fun `Specific financial value and format test`() {
            val financialFormatList = financialFormatJsonList.map { FinancialFormat.fromJSON(it) }

            for (financialFormat in financialFormatList) {
                for (valueMap in financialKeyValueMapList) {
                    val currentFinancialKey = valueMap.keys.first()
                    val currentFinancialValue: Double? = valueMap.values.first() as? Double

                    if (financialFormat.key == currentFinancialKey) {
                        val formattedValue = formatFinancialValue(currentFinancialValue, financialFormat)

                        if (currentFinancialValue == null) {
                            assertEquals("-", formattedValue)
                        } else {
                            assertNotNull(formattedValue)
                            assertFalse { formattedValue == "-" }

                            financialFormat.suffix?.let {
                                if (it.isNotEmpty()) {
                                    assertTrue { formattedValue.contains(it) }
                                }
                            }

                            financialFormat.prefix?.let {
                                if (it.isNotEmpty()) {
                                    assertEquals(it[0].toString(), formattedValue[0].toString())
                                }
                            }

                            if (financialFormat.precision != 0) {
                                val splittedText = formattedValue.split(",")
                                val digitNumberAfterComma = splittedText[1].replace(Regex("[a-zA-Z:s]"), "").trim().length
                                assertEquals(financialFormat.precision, digitNumberAfterComma)
                            }
                        }
                    }
                }
            }
        }
    }
}