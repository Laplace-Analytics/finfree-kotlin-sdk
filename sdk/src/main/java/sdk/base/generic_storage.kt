package sdk.base

import java.time.LocalDateTime

abstract class GenericStorage {
    abstract suspend fun saveBytes(data: List<Int>, path: String)
    abstract suspend fun save(path: String, data: String)
    abstract suspend fun readBytes(path: String): List<Int>?
    abstract suspend fun read(path: String): String?
    abstract suspend fun getLastModified(path: String): LocalDateTime?
    abstract fun clearFolder(path: String)
    abstract fun clearFile(path: String)
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

    override suspend fun readBytes(path: String): List<Int>? {
        return _mapAsStorage[path] as List<Int>?
    }


    override suspend fun save(path: String, data: String) {
        _mapAsStorage[path] = data
    }

    override suspend fun saveBytes(data: List<Int>, path: String) {
        _mapAsStorage[path] = data
    }
}