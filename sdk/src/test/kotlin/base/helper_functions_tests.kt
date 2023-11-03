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
import sdk.base.format
import sdk.base.formatFinancialValue
import sdk.base.formatPercent
import sdk.base.formatPrice
import sdk.base.getAllTimePeriod
import sdk.base.getDoubleFromDynamic
import sdk.base.getOwnedStockCountText
import sdk.base.getTimePeriod
import sdk.base.withEnglishCharacters
import sdk.models.data.assets.Currency
import sdk.models.data.assets.currencySuffix
import java.time.LocalDateTime


const val spaceCharacter:String = " "

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
            val formattedDouble = (256).format(precision = 4)
            assertEquals("256,0000", formattedDouble.trim())

            val formattedDouble2 = null.format(precision = 4, suffix = "₺")
            assertEquals("-", formattedDouble2.trim())

            val formattedDouble3 = 0.format()
            assertEquals("0,00", formattedDouble3.trim())

            val formattedDouble4 = (-12.23).formatPrice(precision = 2, currency = Currency.Tl)
            assertEquals("-₺12,23", formattedDouble4.trim())

            val formattedDouble5 = (9.091).format(precision = 4, suffix = "BTC")
            assertEquals("9,0910BTC", formattedDouble5)

            val formattedDouble6 = (16.0003).format(precision = 3, suffix = "BTC", adjustPrecision = true)
            assertEquals("16,000BTC", formattedDouble6)


            val formattedDouble7 = Currency.Eur.currencySuffix() + (160.0003).format(precision= 3, adjustPrecision= true)

            val formattedDouble8 = (160.0003).formatPrice(precision =  3, currency =  Currency.Eur, adjustPrecision =  true)
            assertEquals(formattedDouble7,formattedDouble8)

            val formattedDouble9 = (160.0003).formatPercent(precision =  3, adjustPrecision =  true)
            assertEquals("%160", formattedDouble9.trim())
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
                    val formattedValue = formatValue.formatPercent(adjustPrecision = true).trim()
                    assertEquals('%', if (formatValue >= 0) formattedValue[0] else formattedValue[1])
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
                    val formattedValue = formatValue.formatPercent(precision = 3,adjustPrecision = false).trim()
                    assertEquals('%', if (formatValue >= 0) formattedValue[0] else formattedValue[1])
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
                    val formattedValue = formatValue.formatPrice(currency = Currency.Tl, adjustPrecision = true).trim()
                    assertEquals('₺', formattedValue[0])
                    val splittedValue = formattedValue.split(",")

                    val tempDigitNumberBeforeComma = splittedValue[0].length - 1
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
                    val formattedValue = formatValue.formatPrice(currency = Currency.Usd).trim()
                    assertEquals('$', formattedValue[0])///todo
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
            val formattedDouble = (12753.34623453).formatPrice(adjustPrecision = true, precision = 3, currency = Currency.Tl)
            assertEquals("₺12.753", formattedDouble.trim())

            val formattedDouble2 = (-23.5).formatPrice(currency = Currency.Usd)
            assertEquals("-${Currency.Usd.currencySuffix()}23,50", formattedDouble2.trim())



            val formattedDouble3 = null.formatPrice(currency = Currency.Usd)
            assertEquals("-", formattedDouble3.trim())


            val formattedDouble4 = (14324.23423).formatPrice(Currency.Tl, prefix = "$", adjustPrecision = true)
            assertEquals("$ ${Currency.Tl.currencySuffix()}14.324", formattedDouble4.trim())

            val formattedDouble5 = (6232343.43).formatPrice(currency = Currency.Tl, prefix = "~", adjustPrecision = true)
            assertEquals("~ ${Currency.Tl.currencySuffix()}6.232.343", formattedDouble5.trim())

            val formattedDouble6 = (4674.82).formatPrice(adjustPrecision = false, precision = 4, currency = Currency.Tl)
            assertEquals("${Currency.Tl.currencySuffix()}4.674,8200", formattedDouble6.trim())

            val formattedDouble7 = (-0.82).formatPrice(precision = 4, currency = Currency.Tl)
            assertEquals("-${Currency.Tl.currencySuffix()}0,8200", formattedDouble7.trim())

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

            val ownedStockCountText7 = getOwnedStockCountText(457457)
            assertEquals("457457", ownedStockCountText7.trim())

            val ownedStockCountText8 = getOwnedStockCountText(0.1)
            assertEquals("0,1", ownedStockCountText8.trim())

            val ownedStockCountText9 = getOwnedStockCountText(1.000001)
            assertEquals("1", ownedStockCountText9.trim())

            val ownedStockCountText10 = getOwnedStockCountText(1.00001)
            assertEquals("1,000", ownedStockCountText10.trim())
        }
    }
    ///todo will be fixed

    //    @Nested
    //    inner class FormatLeaguePrizeTests{
    //        @Test
    //        fun `Basic known usage test`() {
    //            val formattedLeaguePrizeText = formatLeaguePrize(10000.0, currency = Currency.Tl)
    //            assertEquals("10${spaceCharacter}B$spaceCharacter₺", formattedLeaguePrizeText)
    //
    //            val formattedLeaguePrizeText2 = formatLeaguePrize(10500.0, currency = Currency.Tl)
    //            assertEquals("10,5${spaceCharacter}B$spaceCharacter₺", formattedLeaguePrizeText2)
    //
    //            val formattedLeaguePrizeText3 = formatLeaguePrize(19999.0, currency = Currency.Tl)
    //            assertEquals("20${spaceCharacter}B$spaceCharacter₺", formattedLeaguePrizeText3)
    //
    //            val formattedLeaguePrizeText4 = formatLeaguePrize(550000.0, currency = Currency.Tl)
    //           assertEquals("550${spaceCharacter}B$spaceCharacter₺", formattedLeaguePrizeText4)
    //
    //           val formattedLeaguePrizeText5 = formatLeaguePrize(0.0, currency = Currency.Tl)
    //           assertEquals("₺0,00", formattedLeaguePrizeText5)
    //       }
    //   }

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

                            if (financialFormat.suffix != null && financialFormat.suffix!!.isNotEmpty()) {
                                assertEquals(formattedValue.contains(financialFormat.suffix ?: ""), true)
                            }
                            if (financialFormat.prefix != null && financialFormat.prefix!!.isNotEmpty()) {
                                assertEquals(formattedValue[1].toString(), financialFormat.prefix)
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