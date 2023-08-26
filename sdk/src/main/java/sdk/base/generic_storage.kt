package sdk.base

import java.time.LocalDateTime

abstract class GenericStorage {
    abstract suspend fun saveBytes(data: List<Byte>, path: String)
    abstract suspend fun save(path: String, data: String)
    abstract suspend fun readBytes(path: String): List<Byte>?
    abstract suspend fun read(path: String): String?
    abstract suspend fun getLastModified(path: String): LocalDateTime?
    abstract fun clearFolder(path: String)
    abstract fun clearFile(path: String)
}