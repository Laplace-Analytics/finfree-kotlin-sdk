package sdk.base.exceptions

import sdk.models.PortfolioType

open class InitializationException(override val message: String) : Exception() {
    override fun toString(): String {
        return "InitializationException: $message"
    }
}

class SDKNotInitializedException : InitializationException("SDK not initialized, should call FinfreeSDK.initSDK() first")

class NotAuthorizedException : InitializationException("SDK not authorized for user yet, should call FinfreeSDK.userLogin() or FinfreeSDK.authenticateWithRefreshToken() first")

class CoreDataNotInitializedException : InitializationException("Core data not initialized, should call FinfreeSDK.initializeCoreData() first")

class PortfolioHandlerNotInitializedException : InitializationException("Portfolio handler not initialized, should call FinfreeSDK.initializePortfolioHandler() first")

class InvalidPortfolioTypeException(type: PortfolioType) : InitializationException("Invalid portfolio type provided, no portfolio provider with type $type found")
