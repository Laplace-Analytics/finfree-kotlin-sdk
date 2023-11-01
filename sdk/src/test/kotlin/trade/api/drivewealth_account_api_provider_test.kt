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
import sdk.api.LoginResponse
import sdk.api.LoginResponseData
import sdk.api.LoginResponseTypes
import sdk.base.network.BasicResponse
import sdk.base.network.BasicResponseTypes
import sdk.base.network.HTTPHandler
import sdk.trade.DriveWealthAccountApiProvider
import sdk.trade.DriveWealthDocumentTypes

class DriveWealthAccountApiProviderTest {
    private lateinit var authApiProvider: AuthApiProvider
    private lateinit var baseHttpHandler: HTTPHandler
    private lateinit var driveWealthHttpHandler: HTTPHandler
    private lateinit var driveWealthAccountApiProvider: DriveWealthAccountApiProvider
    var gedikUSIban:String = "";


    @BeforeEach
    fun setup() {
        baseHttpHandler = HTTPHandler(httpURL = "finfree.app")
        authApiProvider = AuthApiProvider(baseHttpHandler)
        driveWealthHttpHandler = HTTPHandler(httpURL =  "uat.drivewealth.finfree.app");
        driveWealthAccountApiProvider = DriveWealthAccountApiProvider(driveWealthHttpHandler, "api/v1/tr");
    }
    @Nested
    inner class GetDriveWealthStatementsTests{
        @Test
        fun `Fetch without token state`() = runBlocking {
            val getDriveWealthStatementsResponse: BasicResponse<List<Map<String, Any>>> = driveWealthAccountApiProvider.getDriveWealthDocuments(
                DriveWealthDocumentTypes.Statement
            )
            assertEquals(BasicResponseTypes.Error, getDriveWealthStatementsResponse.responseType)
            assertNull(getDriveWealthStatementsResponse.data)
        }

        @Test
        fun `Fetch with expired token state`() = runBlocking {
            driveWealthHttpHandler.token =
                "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJjb250ZW50X2NyZWF0b3IiOnRydWUsImR3X2FjY291bnRfaWQiOiI4Yzg5OGEyNi05YTU0LTQxMTktOTBiMy0xNTI0NjhjYjU0ZGUuMTY4MTIxMzQ1MTk2MSIsImR3X2FjY291bnRfbm8iOiJGRkFZMDAwMDAxIiwiZXhwIjoxNjkwMzIzNTY3LCJqdXJpc2RpY3Rpb24iOiJ0ciIsImxvY2FsZSI6InRyIiwicmVhZDpmaWx0ZXJfZGV0YWlsIjp0cnVlLCJyZWFkOnJ0X3ByaWNlIjp0cnVlLCJyZWFkOnNlY3RvciI6dHJ1ZSwidXNlcm5hbWUiOiJjbnRya3kifQ.XMOIoR1WdsIUQ9qqy5s31atLv1DfSLeCrijIUNbqrAXCidJI7T39lNM7dGODgofb9gzs9MOfLJr5eateUGHaKw"
            val getDriveWealthStatementsResponse: BasicResponse<List<Map<String, Any>>> = driveWealthAccountApiProvider.getDriveWealthDocuments(
                DriveWealthDocumentTypes.Statement
            )
            assertEquals(BasicResponseTypes.Error, getDriveWealthStatementsResponse.responseType)
            assertNull(getDriveWealthStatementsResponse.data)
        }

        @Test
        fun `Successful state`() = runBlocking {//
            val loginResponse: LoginResponse = authApiProvider.postLogin("KereM", "Kfener2002.")
            if (loginResponse.responseType != LoginResponseTypes.SUCCESS) {
                fail("Could not login to Finfree Account")
            }
            val loginData: LoginResponseData = loginResponse.data!!
            baseHttpHandler.token = loginData.accessToken
            driveWealthHttpHandler.token = loginData.accessToken
            driveWealthHttpHandler.constantHeaders["GEDIK-ACCOUNT-ID"] = "928607"

            val getDriveWealthStatementsResponse: BasicResponse<List<Map<String, Any>>> = driveWealthAccountApiProvider.getDriveWealthDocuments(
                DriveWealthDocumentTypes.Statement
            )
            assertEquals(BasicResponseTypes.Success, getDriveWealthStatementsResponse.responseType)
            assertNotNull(getDriveWealthStatementsResponse.data)
        }

        @Test
        fun `Fetch after token expire scenario`() = runBlocking {
            driveWealthHttpHandler.token =
                "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJjb250ZW50X2NyZWF0b3IiOnRydWUsImR3X2FjY291bnRfaWQiOiI4Yzg5OGEyNi05YTU0LTQxMTktOTBiMy0xNTI0NjhjYjU0ZGUuMTY4MTIxMzQ1MTk2MSIsImR3X2FjY291bnRfbm8iOiJGRkFZMDAwMDAxIiwiZXhwIjoxNjkwMzIzNTY3LCJqdXJpc2RpY3Rpb24iOiJ0ciIsImxvY2FsZSI6InRyIiwicmVhZDpmaWx0ZXJfZGV0YWlsIjp0cnVlLCJyZWFkOnJ0X3ByaWNlIjp0cnVlLCJyZWFkOnNlY3RvciI6dHJ1ZSwidXNlcm5hbWUiOiJjbnRya3kifQ.XMOIoR1WdsIUQ9qqy5s31atLv1DfSLeCrijIUNbqrAXCidJI7T39lNM7dGODgofb9gzs9MOfLJr5eateUGHaKw"
            var getDriveWealthStatementsResponse: BasicResponse<List<Map<String, Any>>> = driveWealthAccountApiProvider.getDriveWealthDocuments(
                DriveWealthDocumentTypes.Statement
            )
            assertEquals(BasicResponseTypes.Error, getDriveWealthStatementsResponse.responseType)
            assertNull(getDriveWealthStatementsResponse.data)

            val loginResponse: LoginResponse = authApiProvider.postLogin("KereM", "Kfener2002.")
            if (loginResponse.responseType != LoginResponseTypes.SUCCESS) {
                fail("Could not login to Finfree Account")
            }
            val loginData: LoginResponseData = loginResponse.data!!
            baseHttpHandler.token = loginData.accessToken
            driveWealthHttpHandler.token = loginData.accessToken
            driveWealthHttpHandler.constantHeaders["GEDIK-ACCOUNT-ID"] = "928607"

            getDriveWealthStatementsResponse = driveWealthAccountApiProvider.getDriveWealthDocuments(
                DriveWealthDocumentTypes.Statement
            )
            assertEquals(BasicResponseTypes.Success, getDriveWealthStatementsResponse.responseType)
            assertNotNull(getDriveWealthStatementsResponse.data)
        }
    }

    @Nested
    inner class GetGedikUSSavedIBANListTests{
        @Test
        fun `Successful state`() = runBlocking {
            val loginResponse: LoginResponse = authApiProvider.postLogin("KereM", "Kfener2002.")
            if (loginResponse.responseType != LoginResponseTypes.SUCCESS) {
                fail("Could not login to Finfree Account")
            }
            val loginData: LoginResponseData = loginResponse.data!!
            baseHttpHandler.token = loginData.accessToken
            driveWealthHttpHandler.token = loginData.accessToken
            driveWealthHttpHandler.constantHeaders["GEDIK-ACCOUNT-ID"] = "928607"

            val getGedikUSSavedIBANListResponse: BasicResponse<List<String>> = driveWealthAccountApiProvider.getGedikUSSavedIBANList()
            assertEquals(BasicResponseTypes.Success, getGedikUSSavedIBANListResponse.responseType)
            assertNotNull(getGedikUSSavedIBANListResponse.data)

            val gedikUSIban = getGedikUSSavedIBANListResponse.data!!.first()
        }

        @Test
        fun `Fetch after token expire scenario`() = runBlocking {
            driveWealthHttpHandler.token =
                "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJjb250ZW50X2NyZWF0b3IiOnRydWUsImR3X2FjY291bnRfaWQiOiI4Yzg5OGEyNi05YTU0LTQxMTktOTBiMy0xNTI0NjhjYjU0ZGUuMTY4MTIxMzQ1MTk2MSIsImR3X2FjY291bnRfbm8iOiJGRkFZMDAwMDAxIiwiZXhwIjoxNjkwMzIzNTY3LCJqdXJpc2RpY3Rpb24iOiJ0ciIsImxvY2FsZSI6InRyIiwicmVhZDpmaWx0ZXJfZGV0YWlsIjp0cnVlLCJyZWFkOnJ0X3ByaWNlIjp0cnVlLCJyZWFkOnNlY3RvciI6dHJ1ZSwidXNlcm5hbWUiOiJjbnRya3kifQ.XMOIoR1WdsIUQ9qqy5s31atLv1DfSLeCrijIUNbqrAXCidJI7T39lNM7dGODgofb9gzs9MOfLJr5eateUGHaKw"
            var getGedikUSSavedIBANListResponse: BasicResponse<List<String>> = driveWealthAccountApiProvider.getGedikUSSavedIBANList()
            assertEquals(BasicResponseTypes.Error, getGedikUSSavedIBANListResponse.responseType)
            assertNull(getGedikUSSavedIBANListResponse.data)

            val loginResponse: LoginResponse = authApiProvider.postLogin("KereM", "Kfener2002.")
            if (loginResponse.responseType != LoginResponseTypes.SUCCESS) {
                fail("Could not login to Finfree Account")
            }
            val loginData: LoginResponseData = loginResponse.data!!
            baseHttpHandler.token = loginData.accessToken
            driveWealthHttpHandler.token = loginData.accessToken
            driveWealthHttpHandler.constantHeaders["GEDIK-ACCOUNT-ID"] = "928607"

            getGedikUSSavedIBANListResponse = driveWealthAccountApiProvider.getGedikUSSavedIBANList()
            assertEquals(BasicResponseTypes.Success, getGedikUSSavedIBANListResponse.responseType)
            assertNotNull(getGedikUSSavedIBANListResponse.data)
        }
    }
    @Nested
    inner class PostGedikUsWithdrawTests{
        @Test
        fun `Post without token state`() = runBlocking {
            val postGedikUSWithdrawResponse: BasicResponse<*> = driveWealthAccountApiProvider.postGedikUSWithdraw(1.0, gedikUSIban)
            assertEquals(BasicResponseTypes.Error, postGedikUSWithdrawResponse.responseType)
        }

        @Test
        fun `Post with expired token state`() = runBlocking {
            driveWealthHttpHandler.token =
                "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJjb250ZW50X2NyZWF0b3IiOnRydWUsImR3X2FjY291bnRfaWQiOiI4Yzg5OGEyNi05YTU0LTQxMTktOTBiMy0xNTI0NjhjYjU0ZGUuMTY4MTIxMzQ1MTk2MSIsImR3X2FjY291bnRfbm8iOiJGRkFZMDAwMDAxIiwiZXhwIjoxNjkwMzIzNTY3LCJqdXJpc2RpY3Rpb24iOiJ0ciIsImxvY2FsZSI6InRyIiwicmVhZDpmaWx0ZXJfZGV0YWlsIjp0cnVlLCJyZWFkOnJ0X3ByaWNlIjp0cnVlLCJyZWFkOnNlY3RvciI6dHJ1ZSwidXNlcm5hbWUiOiJjbnRya3kifQ.XMOIoR1WdsIUQ9qqy5s31atLv1DfSLeCrijIUNbqrAXCidJI7T39lNM7dGODgofb9gzs9MOfLJr5eateUGHaKw"
            val postGedikUSWithdrawResponse: BasicResponse<*> = driveWealthAccountApiProvider.postGedikUSWithdraw(1.0, gedikUSIban)
            assertEquals(BasicResponseTypes.Error, postGedikUSWithdrawResponse.responseType)
        }

        @Test
        fun `Successful state`() = runBlocking {
            val loginResponse: LoginResponse = authApiProvider.postLogin("KereM", "Kfener2002.")
            if (loginResponse.responseType != LoginResponseTypes.SUCCESS) {
                fail("Could not login to Finfree Account")
            }
            val loginData: LoginResponseData = loginResponse.data!!
            baseHttpHandler.token = loginData.accessToken
            driveWealthHttpHandler.token = loginData.accessToken
            driveWealthHttpHandler.constantHeaders["GEDIK-ACCOUNT-ID"] = "928607"

            val postGedikUSWithdrawResponse: BasicResponse<*> = driveWealthAccountApiProvider.postGedikUSWithdraw(1.0, gedikUSIban)
            // insufficient funds
            assertEquals(BasicResponseTypes.Error, postGedikUSWithdrawResponse.responseType)
        }

        @Test
        fun `Post after token expire scenario`() = runBlocking {
            driveWealthHttpHandler.token =
                "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJjb250ZW50X2NyZWF0b3IiOnRydWUsImR3X2FjY291bnRfaWQiOiI4Yzg5OGEyNi05YTU0LTQxMTktOTBiMy0xNTI0NjhjYjU0ZGUuMTY4MTIxMzQ1MTk2MSIsImR3X2FjY291bnRfbm8iOiJGRkFZMDAwMDAxIiwiZXhwIjoxNjkwMzIzNTY3LCJqdXJpc2RpY3Rpb24iOiJ0ciIsImxvY2FsZSI6InRyIiwicmVhZDpmaWx0ZXJfZGV0YWlsIjp0cnVlLCJyZWFkOnJ0X3ByaWNlIjp0cnVlLCJyZWFkOnNlY3RvciI6dHJ1ZSwidXNlcm5hbWUiOiJjbnRya3kifQ.XMOIoR1WdsIUQ9qqy5s31atLv1DfSLeCrijIUNbqrAXCidJI7T39lNM7dGODgofb9gzs9MOfLJr5eateUGHaKw"
            var postGedikUSWithdrawResponse: BasicResponse<*> = driveWealthAccountApiProvider.postGedikUSWithdraw(1.0, gedikUSIban)
            assertEquals(BasicResponseTypes.Error, postGedikUSWithdrawResponse.responseType)

            val loginResponse: LoginResponse = authApiProvider.postLogin("KereM", "Kfener2002.")
            if (loginResponse.responseType != LoginResponseTypes.SUCCESS) {
                fail("Could not login to Finfree Account")
            }
            val loginData: LoginResponseData = loginResponse.data!!
            baseHttpHandler.token = loginData.accessToken
            driveWealthHttpHandler.constantHeaders["GEDIK-ACCOUNT-ID"] = "928607"

            val postGedikUSWithdrawResponse2: BasicResponse<*> = driveWealthAccountApiProvider.postGedikUSWithdraw(1.0, gedikUSIban)
            // insufficient funds
            assertEquals(BasicResponseTypes.Error, postGedikUSWithdrawResponse2.responseType)
        }
    }
}