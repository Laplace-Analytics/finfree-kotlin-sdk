import sdk.base.MockStorage
import sdk.models.data.assets.PortfolioType
import sdk.models.data.assets.Region
import sdk.models.core.AuthenticationResponseTypes
import sdk.models.core.FinfreeSDK

suspend fun initSDK(portfolioType: PortfolioType) {
    FinfreeSDK.initSDK(
        storage = MockStorage(),
        getLocalTimezone = suspend { "Europe/Istanbul" },
    )

    if (!FinfreeSDK.initialized) {
        throw Exception("SDK is not initialized")
    }

    val response = FinfreeSDK.userLogin("test56", "1234qwer")
    if (response.responseType != AuthenticationResponseTypes.Success) {
        throw Exception("Login failed. ResponseType; ${response.responseType}, message: ${response.reasonMessage}")
    }

    if (!FinfreeSDK.authorized) {
        throw Exception("SDK is not authorized")
    }

    FinfreeSDK.initializeCoreData(setOf(Region.Turkish, Region.American))

    if (!FinfreeSDK.coreInitialized) {
        throw Exception("Core is not initialized")
    }
}
