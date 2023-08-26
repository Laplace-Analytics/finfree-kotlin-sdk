package trade.api

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import sdk.api.AuthApiProvider
import sdk.api.LoginResponseTypes
import sdk.api.StockDataPeriods
import sdk.base.network.BasicResponse
import sdk.base.network.BasicResponseTypes
import sdk.base.network.HTTPHandler
import sdk.trade.generic_api.DriveWealthPortfolioApiProvider

class DriveWealthPortfolioApiProviderTest {
    private lateinit var authApiProvider: AuthApiProvider
    private lateinit var baseHttpHandler: HTTPHandler
    private lateinit var driveWealthHttpHandler: HTTPHandler
    private lateinit var driveWealthPortfolioApiProvider: DriveWealthPortfolioApiProvider

    @BeforeEach
    fun setup() {
        baseHttpHandler = HTTPHandler(httpURL = "finfree.app")
        authApiProvider = AuthApiProvider(baseHttpHandler)
        driveWealthHttpHandler = HTTPHandler(httpURL =  "uat.drivewealth.finfree.app");
        driveWealthPortfolioApiProvider = DriveWealthPortfolioApiProvider(driveWealthHttpHandler, "api/v1/tr");
    }

    @Nested
    inner class GetPortfolioTests{
        @Test
        fun fetchWithoutTokenState() = runBlocking {
            val getPortfolioResponse = driveWealthPortfolioApiProvider.getPortfolio()
            assertEquals(BasicResponseTypes.Error, getPortfolioResponse.responseType)
            assertNull(getPortfolioResponse.data)
        }

        @Test
        fun fetchWithExpiredTokenState() = runBlocking {
            driveWealthHttpHandler.token =
                "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJjb250ZW50X2NyZWF0b3IiOnRydWUsImR3X2FjY291bnRfaWQiOiI4Yzg5OGEyNi05YTU0LTQxMTktOTBiMy0xNTI0NjhjYjU0ZGUuMTY4MTIxMzQ1MTk2MSIsImR3X2FjY291bnRfbm8iOiJGRkFZMDAwMDAxIiwiZXhwIjoxNjkwMzIzNTY3LCJqdXJpc2RpY3Rpb24iOiJ0ciIsImxvY2FsZSI6InRyIiwicmVhZDpmaWx0ZXJfZGV0YWlsIjp0cnVlLCJyZWFkOnJ0X3ByaWNlIjp0cnVlLCJyZWFkOnNlY3RvciI6dHJ1ZSwidXNlcm5hbWUiOiJjbnRya3kifQ.XMOIoR1WdsIUQ9qqy5s31atLv1DfSLeCrijIUNbqrAXCidJI7T39lNM7dGODgofb9gzs9MOfLJr5eateUGHaKw"
            val getPortfolioResponse = driveWealthPortfolioApiProvider.getPortfolio()
            assertEquals(BasicResponseTypes.Error, getPortfolioResponse.responseType)
            assertNull(getPortfolioResponse.data)
        }

        @Test
        fun successfulState() = runBlocking {
            val loginResponse = authApiProvider.postLogin("KereM", "Kfener2002.")
            if (loginResponse.responseType != LoginResponseTypes.SUCCESS) {
                fail("Could not login to Finfree Account")
            }
            val loginData = loginResponse.data!!
            baseHttpHandler.token = loginData.accessToken
            driveWealthHttpHandler.token = loginData.accessToken
            driveWealthHttpHandler.constantHeaders["GEDIK-ACCOUNT-ID"] = "928607"

            val getPortfolioResponse = driveWealthPortfolioApiProvider.getPortfolio()
            assertEquals(BasicResponseTypes.Success, getPortfolioResponse.responseType)
            assertNotNull(getPortfolioResponse.data)
        }
        @Test
        fun fetchAfterTokenExpireScenario() = runBlocking {//
            driveWealthHttpHandler.token =
                "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJjb250ZW50X2NyZWF0b3IiOnRydWUsImR3X2FjY291bnRfaWQiOiI4Yzg5OGEyNi05YTU0LTQxMTktOTBiMy0xNTI0NjhjYjU0ZGUuMTY4MTIxMzQ1MTk2MSIsImR3X2FjY291bnRfbm8iOiJGRkFZMDAwMDAxIiwiZXhwIjoxNjkwMzIzNTY3LCJqdXJpc2RpY3Rpb24iOiJ0ciIsImxvY2FsZSI6InRyIiwicmVhZDpmaWx0ZXJfZGV0YWlsIjp0cnVlLCJyZWFkOnJ0X3ByaWNlIjp0cnVlLCJyZWFkOnNlY3RvciI6dHJ1ZSwidXNlcm5hbWUiOiJjbnRya3kifQ.XMOIoR1WdsIUQ9qqy5s31atLv1DfSLeCrijIUNbqrAXCidJI7T39lNM7dGODgofb9gzs9MOfLJr5eateUGHaKw";

            val getPortfolioResponse = driveWealthPortfolioApiProvider.getPortfolio()
            assertEquals(BasicResponseTypes.Error, getPortfolioResponse.responseType)
            assertNull(getPortfolioResponse.data)

            val loginResponse = authApiProvider.postLogin("KereM", "Kfener2002.")
            if (loginResponse.responseType != LoginResponseTypes.SUCCESS) {
                fail("Could not login to Finfree Account")
            }

            val loginData = loginResponse.data!!
            baseHttpHandler.token = loginData.accessToken
            driveWealthHttpHandler.token = loginData.accessToken
            driveWealthHttpHandler.constantHeaders["GEDIK-ACCOUNT-ID"] = "928607"

            val getPortfolioResponse2 = driveWealthPortfolioApiProvider.getPortfolio()
            assertEquals(BasicResponseTypes.Success, getPortfolioResponse2.responseType)
            assertNotNull(getPortfolioResponse2.data)
        }
    }
    @Nested
    inner class GetEquityDataTests{
        @Test
        fun fetchWithExpiredTokenState() = runBlocking {
            driveWealthHttpHandler.token =
                "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJjb250ZW50X2NyZWF0b3IiOnRydWUsImR3X2FjY291bnRfaWQiOiI4Yzg5OGEyNi05YTU0LTQxMTktOTBiMy0xNTI0NjhjYjU0ZGUuMTY4MTIxMzQ1MTk2MSIsImR3X2FjY291bnRfbm8iOiJGRkFZMDAwMDAxIiwiZXhwIjoxNjkwMzIzNTY3LCJqdXJpc2RpY3Rpb24iOiJ0ciIsImxvY2FsZSI6InRyIiwicmVhZDpmaWx0ZXJfZGV0YWlsIjp0cnVlLCJyZWFkOnJ0X3ByaWNlIjp0cnVlLCJyZWFkOnNlY3RvciI6dHJ1ZSwidXNlcm5hbWUiOiJjbnRya3kifQ.XMOIoR1WdsIUQ9qqy5s31atLv1DfSLeCrijIUNbqrAXCidJI7T39lNM7dGODgofb9gzs9MOfLJr5eateUGHaKw"

            val getEquityDataResponse: BasicResponse<List<Map<String, Any>>> =
                driveWealthPortfolioApiProvider.getEquityData(StockDataPeriods.Price1M)

            assertEquals(BasicResponseTypes.Error, getEquityDataResponse.responseType)
            assertNull(getEquityDataResponse.data)
        }

        @Test
        fun successfulState() = runBlocking {
            val loginResponse = authApiProvider.postLogin("KereM", "Kfener2002.")
            if (loginResponse.responseType != LoginResponseTypes.SUCCESS) {
                fail("Could not login to Finfree Account")
            }

            val loginData = loginResponse.data!!
            baseHttpHandler.token = loginData.accessToken
            driveWealthHttpHandler.token = loginData.accessToken
            driveWealthHttpHandler.constantHeaders["GEDIK-ACCOUNT-ID"] = "928607"

            val getEquityDataResponse: BasicResponse<List<Map<String, Any>>> =
                driveWealthPortfolioApiProvider.getEquityData(StockDataPeriods.Price1M)

            assertEquals(BasicResponseTypes.Success, getEquityDataResponse.responseType)
            assertNotNull(getEquityDataResponse.data)
        }

        @Test
        fun fetchAfterTokenExpireScenario() = runBlocking {
            driveWealthHttpHandler.token =
                "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJjb250ZW50X2NyZWF0b3IiOnRydWUsImR3X2FjY291bnRfaWQiOiI4Yzg5OGEyNi05YTU0LTQxMTktOTBiMy0xNTI0NjhjYjU0ZGUuMTY4MTIxMzQ1MTk2MSIsImR3X2FjY291bnRfbm8iOiJGRkFZMDAwMDAxIiwiZXhwIjoxNjkwMzIzNTY3LCJqdXJpc2RpY3Rpb24iOiJ0ciIsImxvY2FsZSI6InRyIiwicmVhZDpmaWx0ZXJfZGV0YWlsIjp0cnVlLCJyZWFkOnJ0X3ByaWNlIjp0cnVlLCJyZWFkOnNlY3RvciI6dHJ1ZSwidXNlcm5hbWUiOiJjbnRya3kifQ.XMOIoR1WdsIUQ9qqy5s31atLv1DfSLeCrijIUNbqrAXCidJI7T39lNM7dGODgofb9gzs9MOfLJr5eateUGHaKw"

            val getEquityDataResponse: BasicResponse<List<Map<String, Any>>> =
                driveWealthPortfolioApiProvider.getEquityData(StockDataPeriods.Price1M)

            assertEquals(BasicResponseTypes.Error, getEquityDataResponse.responseType)
            assertNull(getEquityDataResponse.data)

            val loginResponse = authApiProvider.postLogin("KereM", "Kfener2002.")
            if (loginResponse.responseType != LoginResponseTypes.SUCCESS) {
                fail("Could not login to Finfree Account")
            }

            val loginData = loginResponse.data!!
            baseHttpHandler.token = loginData.accessToken
            driveWealthHttpHandler.token = loginData.accessToken
            driveWealthHttpHandler.constantHeaders["GEDIK-ACCOUNT-ID"] = "928607"

            val getEquityDataResponse2: BasicResponse<List<Map<String, Any>>> =
                driveWealthPortfolioApiProvider.getEquityData(StockDataPeriods.Price1M)

            assertEquals(BasicResponseTypes.Success, getEquityDataResponse2.responseType)
            assertNotNull(getEquityDataResponse2.data)
        }
    }
}