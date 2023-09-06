package sdk.base

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import sdk.base.network.GenericApiProvider
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime

abstract class GenericRepository<T, V, K : GenericApiProvider>(
    open val storageHandler: GenericStorage,
    open val apiProvider: K,
    val cacheDuration: Duration = Duration.ofDays(1)
) {

    abstract suspend fun fetchData(identifier: V?): T?
    abstract fun getPath(identifier: V?): String
    abstract fun getIdentifier(data: T): V?
    abstract fun getFromJson(json: Map<String, Any>): T
    abstract fun toJson(data: T): Map<String, Any>

    open suspend fun getData(identifier: V?): T? {
        try {
            val data = readData(identifier)
            data?.let { return it }
        } catch (e: NotImplementedError) {
            // do nothing
        }
        catch (ex: Exception) {
            logger.error("Error reading data from local storage", ex)
        }

        val response = fetchData(identifier)
        response?.let {
            try {
                saveData(it)
            } catch (e: NotImplementedError) {
                // do nothing
            }
            catch (ex: Exception) {
                logger.error("Error saving data to local storage", ex)
            }
        }
        return response
    }

    open suspend fun readData(identifier: V?): T? {
        try {
            val path = getPath(identifier)
            val lastUpdated = storageHandler.getLastModified(path)

            if (lastUpdated == null || lastUpdated.isBefore(LocalDateTime.now().minus(cacheDuration))) {
                return null
            }

            val data = storageHandler.read(path) ?: return null
            val type = object : TypeToken<Map<String, Any>>() {}.type

            val resultMap: Map<String, Any> = Gson().fromJson(data,type)

            return getFromJson(resultMap) // Replace with appropriate Kotlin JSON parsing method
        } catch (e: NotImplementedError) {
            // do nothing
        }
        catch (ex: Exception) {
            logger.error("Error reading data from local storage", ex)
        }
        return null
    }

    suspend fun saveData(data: T) {
        try {
            storageHandler.save(getPath(getIdentifier(data)), Gson().toJson(toJson(data)))
        } catch (e: NotImplementedError) {
            // do nothing
        }
        catch (ex: Exception) {
            logger.error("Error saving data to local storage", ex)
        }
    }
}
