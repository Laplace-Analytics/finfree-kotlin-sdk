import sdk.base.GenericStorage
import sdk.models.PortfolioType
import sdk.models.Region
import sdk.models.core.AuthenticationResponseTypes
import sdk.models.core.FinfreeSDK
import sdk.trade.models.portfolio.DWPortfolioHandler
import java.time.LocalDateTime

suspend fun initSDK(portfolioType: PortfolioType) {
    FinfreeSDK.initSDK(
        storage = MockStorage(),
        getLocalTimezone = suspend { "Europe/Istanbul" },
        portfolioHandlers = mapOf(
            portfolioType to DWPortfolioHandler(driveWealthUATURL)
        )
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

    FinfreeSDK.initializeCoreData(setOf(Region.turkish,Region.american))

    if (!FinfreeSDK.coreInitialized) {
        throw Exception("Core is not initialized")
    }
}

class MockStorage : GenericStorage() {
    private val _mapAsStorage = mutableMapOf<String, Any>()

    override fun clearFolder(path: String) {
        _mapAsStorage.remove(path)
    }

    override fun clearFile(path: String) {
        _mapAsStorage.remove(path)
    }

    override suspend fun getLastModified(path: String): LocalDateTime? {
        return LocalDateTime.now()
    }

    override suspend fun read(path: String): String? {
        return _mapAsStorage[path] as? String
    }

    override suspend fun readBytes(path: String): List<Byte>? {
        return _mapAsStorage[path] as? List<Byte>
    }


    override suspend fun save(path: String, data: String) {
        _mapAsStorage[path] = data
    }

    override suspend fun saveBytes(data: List<Byte>, path: String) {
        _mapAsStorage[path] = data
    }
}
