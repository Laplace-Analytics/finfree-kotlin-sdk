package trade.api

import kotlinx.coroutines.delay
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
import sdk.base.network.BasicResponseTypes
import sdk.base.network.HTTPHandler
import sdk.models.data.assets.Asset
import sdk.models.data.assets.AssetType
import sdk.models.data.assets.Region
import sdk.trade.api.drivewealth_api.DriveWealthOrderAPIProvider
import java.time.LocalDateTime

class DriveWealthOrderAPIProviderTests{
        private lateinit var authApiProvider: AuthApiProvider
        private lateinit var baseHttpHandler: HTTPHandler
        private lateinit var driveWealthHttpHandler: HTTPHandler
        private lateinit var driveWealthOrderAPIProvider: DriveWealthOrderAPIProvider

    val appleAsset = Asset(
        symbol = "AAPL",
        id = "",
        name = "",
        industryId = "",
        sectorId = "",
        region = Region.American,
        isActive = true,
        type = AssetType.Stock,
        tradable = true
    )

    val dennAsset = Asset(
        symbol = "DENN",
        id = "",
        name = "",
        industryId = "",
        sectorId = "",
        region = Region.American,
        isActive = true,
        type = AssetType.Stock,
        tradable = true
    )

    val aanAsset = Asset(
        symbol = "AAN",
        id = "",
        name = "",
        industryId = "",
        sectorId = "",
        region = Region.American,
        isActive = true,
        type = AssetType.Stock,
        tradable = true
    )

    val bmlnAsset = Asset(
        symbol = "BLMN",
        id = "",
        name = "",
        industryId = "",
        sectorId = "",
        region = Region.American,
        isActive = true,
        type = AssetType.Stock,
        tradable = true
    )


    @BeforeEach
        fun setup() {
            baseHttpHandler = HTTPHandler(httpURL = "finfree.app")
            authApiProvider = AuthApiProvider(baseHttpHandler)
            driveWealthHttpHandler = HTTPHandler(httpURL =  "uat.drivewealth.finfree.app");
            driveWealthOrderAPIProvider = DriveWealthOrderAPIProvider(driveWealthHttpHandler, "api/v1/tr/order");
        }

    @Nested
    inner class PostLimitOrderTests{
        @Test
        fun `Post without token state`() = runBlocking {
            val postLimitOrderResponse = driveWealthOrderAPIProvider.postLimitOrder(
                1,
                appleAsset,
                10.0
            )
            assertEquals(BasicResponseTypes.Error, postLimitOrderResponse.responseType)
            assertNull(postLimitOrderResponse.data)

            delay(1000)

            val postLimitOrderResponse2 = driveWealthOrderAPIProvider.postLimitOrder(
                1,
                dennAsset,
                10.0
            )
            assertEquals(BasicResponseTypes.Error, postLimitOrderResponse2.responseType)
            assertNull(postLimitOrderResponse2.data)
        }
        @Test
        fun `Post with expired token state`() = runBlocking {
            driveWealthHttpHandler.token = "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJjb250ZW50X2NyZWF0b3IiOnRydWUsImR3X2FjY291bnRfaWQiOiI4Yzg5OGEyNi05YTU0LTQxMTktOTBiMy0xNTI0NjhjYjU0ZGUuMTY4MTIxMzQ1MTk2MSIsImR3X2FjY291bnRfbm8iOiJGRkFZMDAwMDAxIiwiZXhwIjoxNjkwMzIzNTY3LCJqdXJpc2RpY3Rpb24iOiJ0ciIsImxvY2FsZSI6InRyIiwicmVhZDpmaWx0ZXJfZGV0YWlsIjp0cnVlLCJyZWFkOnJ0X3ByaWNlIjp0cnVlLCJyZWFkOnNlY3RvciI6dHJ1ZSwidXNlcm5hbWUiOiJjbnRya3kifQ.XMOIoR1WdsIUQ9qqy5s31atLv1DfSLeCrijIUNbqrAXCidJI7T39lNM7dGODgofb9gzs9MOfLJr5eateUGHaKw"
            val postLimitOrderResponse = driveWealthOrderAPIProvider.postLimitOrder(
                1,
                appleAsset,
                10.0
            )
            assertEquals(BasicResponseTypes.Error, postLimitOrderResponse.responseType)
            assertNull(postLimitOrderResponse.data)

            delay(1000)

            val postLimitOrderResponse2 = driveWealthOrderAPIProvider.postLimitOrder(
                1,
                dennAsset,
                10.0
            )
            assertEquals(BasicResponseTypes.Error, postLimitOrderResponse2.responseType)
            assertNull(postLimitOrderResponse2.data)
        }

        @Test
        fun `Successful state`() = runBlocking {

             val loginResponse =  authApiProvider.postLogin("KereM", "Kfener2002.")
            if (loginResponse.responseType != LoginResponseTypes.SUCCESS) {
                fail("Could not login to Finfree Account")
            }
            val loginData = loginResponse.data!!
            baseHttpHandler.token = loginData.accessToken
            driveWealthHttpHandler.token = loginData.accessToken
            driveWealthHttpHandler.constantHeaders["GEDIK-ACCOUNT-ID"] = "928607"
            val postLimitOrderResponse = driveWealthOrderAPIProvider.postLimitOrder(
                1,
                appleAsset,
                10.2
            )
            assertEquals(BasicResponseTypes.Success, postLimitOrderResponse.responseType)
            assertNotNull(postLimitOrderResponse.data)

            delay(3000)

            val postLimitOrderResponse2 = driveWealthOrderAPIProvider.postLimitOrder(
                1,
                dennAsset,
                10.2
            )
            assertEquals(BasicResponseTypes.Success, postLimitOrderResponse2.responseType)
            assertNotNull(postLimitOrderResponse2.data)
        }

        @Test
        fun `Post after token expire scenario`() = runBlocking {
            driveWealthHttpHandler.token =
                "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJjb250ZW50X2NyZWF0b3IiOnRydWUsImR3X2FjY291bnRfaWQiOiI4Yzg5OGEyNi05YTU0LTQxMTktOTBiMy0xNTI0NjhjYjU0ZGUuMTY4MTIxMzQ1MTk2MSIsImR3X2FjY291bnRfbm8iOiJGRkFZMDAwMDAxIiwiZXhwIjoxNjkwMzIzNTY3LCJqdXJpc2RpY3Rpb24iOiJ0ciIsImxvY2FsZSI6InRyIiwicmVhZDpmaWx0ZXJfZGV0YWlsIjp0cnVlLCJyZWFkOnJ0X3ByaWNlIjp0cnVlLCJyZWFkOnNlY3RvciI6dHJ1ZSwidXNlcm5hbWUiOiJjbnRya3kifQ.XMOIoR1WdsIUQ9qqy5s31atLv1DfSLeCrijIUNbqrAXCidJI7T39lNM7dGODgofb9gzs9MOfLJr5eateUGHaKw"
            val postLimitOrderResponse = driveWealthOrderAPIProvider.postLimitOrder(
                1,
                appleAsset,
                10.0
            )
            assertEquals(BasicResponseTypes.Error, postLimitOrderResponse.responseType)
            assertNull(postLimitOrderResponse.data)

            delay(1000)

            val postLimitOrderResponse2 = driveWealthOrderAPIProvider.postLimitOrder(
                1,
                dennAsset,
                10.0
            )
            assertEquals(BasicResponseTypes.Error, postLimitOrderResponse2.responseType)
            assertNull(postLimitOrderResponse2.data)

            val loginResponse =  authApiProvider.postLogin("KereM", "Kfener2002.")
            if (loginResponse.responseType != LoginResponseTypes.SUCCESS) {
                fail("Could not login to Finfree Account")
            }
            val loginData = loginResponse.data!!
            baseHttpHandler.token = loginData.accessToken
            driveWealthHttpHandler.token = loginData.accessToken
            driveWealthHttpHandler.constantHeaders["GEDIK-ACCOUNT-ID"] = "928607"
            val postLimitOrderResponse3 = driveWealthOrderAPIProvider.postLimitOrder(
                1,
                aanAsset,
                10.2
            )
            assertEquals(BasicResponseTypes.Success, postLimitOrderResponse3.responseType)
            assertNotNull(postLimitOrderResponse3.data)

            delay(1000)

            val postLimitOrderResponse4 = driveWealthOrderAPIProvider.postLimitOrder(
                1,
                appleAsset,
                10.2
            )
            assertEquals(BasicResponseTypes.Success, postLimitOrderResponse4.responseType)
            assertNotNull(postLimitOrderResponse4.data)
        }
    }

    @Nested
    inner class PostMarketOrderTests{
        @Test
        fun `Post without token state`() = runBlocking {
            val postMarketOrderResponse = driveWealthOrderAPIProvider.postMarketOrder(
                0.01,
                appleAsset,
            )
            assertEquals(BasicResponseTypes.Error, postMarketOrderResponse.responseType)
            assertNull(postMarketOrderResponse.data)

            delay(1000)

            val postLimitOrderResponse2 = driveWealthOrderAPIProvider.postMarketOrder(
                0.01,
                appleAsset,
            )
            assertEquals(BasicResponseTypes.Error, postLimitOrderResponse2.responseType)
            assertNull(postLimitOrderResponse2.data)
        }
        @Test
        fun `Post with expired token state`() = runBlocking {
            driveWealthHttpHandler.token =
                "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJjb250ZW50X2NyZWF0b3IiOnRydWUsImR3X2FjY291bnRfaWQiOiI4Yzg5OGEyNi05YTU0LTQxMTktOTBiMy0xNTI0NjhjYjU0ZGUuMTY4MTIxMzQ1MTk2MSIsImR3X2FjY291bnRfbm8iOiJGRkFZMDAwMDAxIiwiZXhwIjoxNjkwMzIzNTY3LCJqdXJpc2RpY3Rpb24iOiJ0ciIsImxvY2FsZSI6InRyIiwicmVhZDpmaWx0ZXJfZGV0YWlsIjp0cnVlLCJyZWFkOnJ0X3ByaWNlIjp0cnVlLCJyZWFkOnNlY3RvciI6dHJ1ZSwidXNlcm5hbWUiOiJjbnRya3kifQ.XMOIoR1WdsIUQ9qqy5s31atLv1DfSLeCrijIUNbqrAXCidJI7T39lNM7dGODgofb9gzs9MOfLJr5eateUGHaKw"

            val postMarketOrderResponse = driveWealthOrderAPIProvider.postMarketOrder(
                0.01,
                appleAsset,
            )
            assertEquals(BasicResponseTypes.Error, postMarketOrderResponse.responseType)
            assertNull(postMarketOrderResponse.data)

            delay(1000)

            val postMarketOrderResponse2 = driveWealthOrderAPIProvider.postMarketOrder(
                0.01,
                appleAsset,
            )
            assertEquals(BasicResponseTypes.Error, postMarketOrderResponse2.responseType)
            assertNull(postMarketOrderResponse2.data)
        }
        @Test
        fun `Successful state`() = runBlocking {

            val loginResponse =  authApiProvider.postLogin("KereM", "Kfener2002.")
            if (loginResponse.responseType != LoginResponseTypes.SUCCESS) {
                fail("Could not login to Finfree Account")
            }
            val loginData = loginResponse.data!!
            baseHttpHandler.token = loginData.accessToken
            driveWealthHttpHandler.token = loginData.accessToken
            driveWealthHttpHandler.constantHeaders["GEDIK-ACCOUNT-ID"] = "928607"
            val postMarketOrderResponse = driveWealthOrderAPIProvider.postMarketOrder(
                0.01,
                dennAsset,
            )
            val currentTime = LocalDateTime.now()

            if (currentTime.isAfter(LocalDateTime.of(currentTime.year, currentTime.monthValue, currentTime.dayOfMonth, 16, 30)) &&
                currentTime.isBefore(LocalDateTime.of(currentTime.year, currentTime.monthValue, currentTime.dayOfMonth, 22, 59))) {

                assertEquals(BasicResponseTypes.Success, postMarketOrderResponse.responseType)
                assertNotNull(postMarketOrderResponse.data)
            } else {
                assertEquals(BasicResponseTypes.Error, postMarketOrderResponse.responseType)
                assertNull(postMarketOrderResponse.data)
            }
            delay(1000)

            val postMarketOrderResponse2 = driveWealthOrderAPIProvider.postMarketOrder(
                0.01,
                dennAsset,
            )

            val currentTime2 = LocalDateTime.now()

            if (currentTime2.isAfter(LocalDateTime.of(currentTime2.year, currentTime2.monthValue, currentTime2.dayOfMonth, 16, 30)) &&
                currentTime2.isBefore(LocalDateTime.of(currentTime2.year, currentTime2.monthValue, currentTime2.dayOfMonth, 22, 59))) {

                assertEquals(BasicResponseTypes.Success, postMarketOrderResponse.responseType)
                assertNotNull(postMarketOrderResponse.data)

            } else {
                assertEquals(BasicResponseTypes.Error, postMarketOrderResponse.responseType)
                assertNull(postMarketOrderResponse.data)
            }
            assertEquals(BasicResponseTypes.Success, postMarketOrderResponse2.responseType)
            assertNotNull(postMarketOrderResponse2.data)
        }
    }
}
