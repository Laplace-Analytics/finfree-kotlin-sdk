package models.core.sessions

import MockStorage
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import sdk.api.AuthApiProvider
import sdk.api.CoreApiProvider
import sdk.api.LoginResponseTypes
import sdk.base.network.HTTPHandler
import sdk.models.AssetClass
import sdk.models.Region
import sdk.models.core.SessionProvider
import sdk.repositories.SessionsRepo
import java.time.LocalDateTime

class SessionProviderTest{
    private lateinit var authApiProvider: AuthApiProvider
    private lateinit var baseHttpHandler: HTTPHandler
    private lateinit var coreApiProvider: CoreApiProvider
    private lateinit var sessionsRepo: SessionsRepo
    private lateinit var sessionProvider: SessionProvider

    @BeforeEach
    fun setup() {
        baseHttpHandler = HTTPHandler(httpURL = "finfree.app")
        authApiProvider = AuthApiProvider(baseHttpHandler)
        coreApiProvider = CoreApiProvider(baseHttpHandler)


        sessionsRepo = SessionsRepo(
            MockStorage(),
            coreApiProvider
        ) { "Europe/Istanbul" };
        sessionProvider = SessionProvider(sessionsRepo = sessionsRepo);
    }
    @Test
    fun `getDayStart test`() = runBlocking {
        val loginResponse = authApiProvider.postLogin("test44", "1234qwer")
        if (loginResponse.responseType != LoginResponseTypes.SUCCESS) {
            fail("Could not login to Finfree Account")
        }
        val loginData = loginResponse.data!!
        baseHttpHandler.token = loginData.accessToken

        sessionProvider.init()

        val now = LocalDateTime.now()
        val dayStartDateDefaultValues = sessionProvider.getDayStart()
        assertEquals(10, dayStartDateDefaultValues.hour)
        assertEquals(25, dayStartDateDefaultValues.dayOfMonth)
        assertEquals(now.month, dayStartDateDefaultValues.month)

        val dayStartDateAmericanEquity = sessionProvider.getDayStart(region = Region.american, assetClass = AssetClass.equity)
        assertEquals(16, dayStartDateAmericanEquity.hour)//not passed
        assertEquals(30, dayStartDateAmericanEquity.minute)
        assertEquals(now.month, dayStartDateAmericanEquity.month)

        val dayStartDateTurkishEquity = sessionProvider.getDayStart(region = Region.turkish, assetClass = AssetClass.equity)
        assertEquals(10, dayStartDateTurkishEquity.hour)
        assertEquals(now.month, dayStartDateTurkishEquity.month)

        val dayStartDateTurkishCrypto = sessionProvider.getDayStart(region = Region.turkish, assetClass = AssetClass.crypto)
        assertEquals(0, dayStartDateTurkishCrypto.hour)
        assertEquals(now.dayOfMonth, dayStartDateTurkishCrypto.dayOfMonth)
        assertEquals(now.month, dayStartDateTurkishCrypto.month)

        val marketClosedDate = LocalDateTime.of(2023, 8, 7, 20, 30)
        val dayStartDateTurkishEquitySpecificDate = sessionProvider.getDayStart(
            region = Region.turkish,
            assetClass = AssetClass.equity,
            date = marketClosedDate
        )
        assertEquals(10, dayStartDateTurkishEquitySpecificDate.hour)
        assertEquals(marketClosedDate.plusDays(1).dayOfMonth, dayStartDateTurkishEquitySpecificDate.dayOfMonth)
        assertEquals(now.month, dayStartDateTurkishEquitySpecificDate.month)

        val marketOpenDate = LocalDateTime.of(2023, 8, 7, 15, 30)
        val dayStartDateTurkishEquitySpecificDate2 = sessionProvider.getDayStart(
            region = Region.turkish,
            assetClass = AssetClass.equity,
            date = marketOpenDate
        )
        assertEquals(dayStartDateTurkishEquitySpecificDate.hour, dayStartDateTurkishEquitySpecificDate2.hour)
        assertEquals(10, dayStartDateTurkishEquitySpecificDate2.hour)
        assertEquals(marketOpenDate.dayOfMonth, dayStartDateTurkishEquitySpecificDate2.dayOfMonth)
        assertEquals(now.month, dayStartDateTurkishEquitySpecificDate2.month)
    }
    @Test
    fun `getDayEnd test`() = runBlocking {
        val loginResponse = authApiProvider.postLogin("test44", "1234qwer")
        if (loginResponse.responseType != LoginResponseTypes.SUCCESS) {
            fail("Could not login to Finfree Account")
        }
        val loginData = loginResponse.data!!
        baseHttpHandler.token = loginData.accessToken

        sessionProvider.init()
        val now = LocalDateTime.now()
        val dayEndDateDefaultValues = sessionProvider.getDayEnd()
        assertEquals(18, dayEndDateDefaultValues.hour)
        assertEquals(now.monthValue, dayEndDateDefaultValues.monthValue)

        val dayEndDateAmericanEquity = sessionProvider.getDayEnd(region = Region.american, assetClass = AssetClass.equity)
        assertEquals(23, dayEndDateAmericanEquity.hour)//not passed

        val dayEndDateTurkishEquity = sessionProvider.getDayEnd(region = Region.turkish, assetClass = AssetClass.equity)
        assertEquals(18, dayEndDateTurkishEquity.hour)
        assertEquals(25, dayEndDateTurkishEquity.dayOfMonth)
        assertEquals(now.monthValue, dayEndDateTurkishEquity.monthValue)

        val dayEndDateTurkishCrypto = sessionProvider.getDayEnd(region = Region.turkish, assetClass = AssetClass.crypto)
        assertEquals(23, dayEndDateTurkishCrypto.hour)
        assertEquals(now.dayOfMonth, dayEndDateTurkishCrypto.dayOfMonth)
        assertEquals(now.monthValue, dayEndDateTurkishCrypto.monthValue)

        val marketClosedDate = LocalDateTime.of(2023, 8, 7, 20, 30)
        val dayEndDateTurkishEquitySpecificDate = sessionProvider.getDayEnd(
            region = Region.turkish,
            assetClass = AssetClass.equity,
            date = marketClosedDate
        )
        assertEquals(18, dayEndDateTurkishEquitySpecificDate.hour)
        assertEquals(marketClosedDate.dayOfMonth, dayEndDateTurkishEquitySpecificDate.dayOfMonth)
        assertEquals(now.monthValue, dayEndDateTurkishEquitySpecificDate.monthValue)

        val marketOpenDate = LocalDateTime.of(2023, 8, 7, 15, 30)
        val dayEndDateTurkishEquitySpecificDate2 = sessionProvider.getDayEnd(
            region = Region.turkish,
            assetClass = AssetClass.equity,
            date = marketOpenDate
        )
        assertEquals(dayEndDateTurkishEquitySpecificDate.hour, dayEndDateTurkishEquitySpecificDate2.hour)
        assertEquals(18, dayEndDateTurkishEquitySpecificDate2.hour)
        assertEquals(marketOpenDate.dayOfMonth, dayEndDateTurkishEquitySpecificDate2.dayOfMonth)
        assertEquals(now.monthValue, dayEndDateTurkishEquitySpecificDate2.monthValue)

        val marketClosedDate2 = LocalDateTime.of(2023, 8, 8, 8, 30)
        val dayEndDateTurkishEquitySpecificDate3 = sessionProvider.getDayEnd(
            region = Region.turkish,
            assetClass = AssetClass.equity,
            date = marketClosedDate2
        )
        assertEquals(18, dayEndDateTurkishEquitySpecificDate3.hour)
        assertEquals(marketClosedDate2.dayOfMonth - 1, dayEndDateTurkishEquitySpecificDate3.dayOfMonth)
        assertEquals(now.monthValue, dayEndDateTurkishEquitySpecificDate3.monthValue)
    }

    @Test
    fun `isDuringMarketHours test`() = runBlocking {
        val loginResponse = authApiProvider.postLogin("KereM", "Kfener2002.")
        if (loginResponse.responseType != LoginResponseTypes.SUCCESS) {
            fail("Could not login to Finfree Account")
        }
        val loginData = loginResponse.data!!
        baseHttpHandler.token = loginData.accessToken

        sessionProvider.init()

        val turkishMarketClosedDate = LocalDateTime.of(2023, 8, 8, 8, 30)
        val isDuringMarketHours = sessionProvider.isDuringMarketHours(date = turkishMarketClosedDate)
        assertEquals(false, isDuringMarketHours)

        val turkishMarketClosedDate2 = LocalDateTime.of(2023, 8, 8, 18, 15, 1)
        val isDuringMarketHours2 = sessionProvider.isDuringMarketHours(date = turkishMarketClosedDate2)
        assertEquals(false, isDuringMarketHours2)

        val turkishMarketOpenDate = LocalDateTime.of(2023, 8, 8, 18, 15)
        val isDuringMarketHours3 = sessionProvider.isDuringMarketHours(date = turkishMarketOpenDate)
        assertEquals(true, isDuringMarketHours3)

        val turkishMarketOpenDate2 = LocalDateTime.of(2023, 8, 8, 13, 15)
        val isDuringMarketHours4 = sessionProvider.isDuringMarketHours(date = turkishMarketOpenDate2)
        assertEquals(true, isDuringMarketHours4)

        val weekendDate = LocalDateTime.of(2023, 8, 6, 13, 15)
        val isDuringMarketHours5 = sessionProvider.isDuringMarketHours(date = weekendDate)
        assertEquals(false, isDuringMarketHours5)

        val americanMarketClosedDate = LocalDateTime.of(2023, 8, 8, 8, 30)
        val isDuringMarketHours6 = sessionProvider.isDuringMarketHours(date = americanMarketClosedDate, region = Region.american)
        assertEquals(false, isDuringMarketHours6)

        val americanMarketClosedDate2 = LocalDateTime.of(2023, 8, 8, 23, 0, 1)
        val isDuringMarketHours7 = sessionProvider.isDuringMarketHours(date = americanMarketClosedDate2, region = Region.american)
        assertEquals(false, isDuringMarketHours7)

        val americanMarketOpenDate = LocalDateTime.of(2023, 8, 8, 22, 59,0)
        val isDuringMarketHours8 = sessionProvider.isDuringMarketHours(date = americanMarketOpenDate, region = Region.american)
        assertEquals(true, isDuringMarketHours8)

        val americanMarketOpenDate2 = LocalDateTime.of(2023, 8, 8, 19, 15)
        val isDuringMarketHours9 = sessionProvider.isDuringMarketHours(date = americanMarketOpenDate2, region = Region.american)
        assertEquals(true, isDuringMarketHours9)

        val americanCryptoMarketOpenDate = LocalDateTime.of(2023, 8, 8, 22, 59)
        val isDuringMarketHours10 = sessionProvider.isDuringMarketHours(date = americanCryptoMarketOpenDate, region = Region.american, assetClass = AssetClass.crypto)
        assertEquals(false, isDuringMarketHours10)

        val turkishCryptoMarketOpenDate = LocalDateTime.of(2023, 8, 8, 19, 15)
        val isDuringMarketHours11 = sessionProvider.isDuringMarketHours(date = turkishCryptoMarketOpenDate, region = Region.turkish, assetClass = AssetClass.crypto)
        assertEquals(true, isDuringMarketHours11)

        val turkishCryptoMarketOpenDate2 = LocalDateTime.of(2023, 8, 8, 0, 0)
        val isDuringMarketHours12 = sessionProvider.isDuringMarketHours(date = turkishCryptoMarketOpenDate2, region = Region.turkish, assetClass = AssetClass.crypto)
        assertEquals(true, isDuringMarketHours12)
    }
}