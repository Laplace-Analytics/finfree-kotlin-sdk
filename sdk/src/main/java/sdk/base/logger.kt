package sdk.base

import com.sun.org.slf4j.internal.Logger
import com.sun.org.slf4j.internal.LoggerFactory



val logger = CustomLogger()

class CustomLogger {
    private val logger: Logger = LoggerFactory.getLogger(CustomLogger::class.java)

    fun error(message: Any, error: Any? = null) {
        logger.error("$message, $error")
    }

    fun warning(message: Any, error: Any? = null) {
        logger.warn("$message, $error")
    }
}