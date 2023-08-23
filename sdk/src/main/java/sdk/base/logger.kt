package sdk.base

import org.slf4j.Logger
import org.slf4j.LoggerFactory


val logger = CustomLogger()

class CustomLogger {
    private val logger: Logger = LoggerFactory.getLogger(CustomLogger::class.java)

    fun error(message: Any, error: Any? = null) {
        logger.error("$message, $error")
    }

    fun warning(message: Any, error: Any? = null) {
        logger.warn("$message, $error")
    }

    fun info(message: Any){
        logger.info("$message")
    }
}