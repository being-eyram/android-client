package com.speechly.client.device

import com.speechly.client.cache.CacheService
import java.util.UUID

/**
 * Speechly device identifier type.
 *
 * Device identifiers are required by the SLU API for configuring the recognition model.
 *
 * A valid device identifier is represented by an UUIDv4 string.
 */
typealias DeviceId = UUID

/**
 * An interface that provides Speechly device identifiers to be consumed by the SLU API client.
 */
interface DeviceIdProvider {
    /**
     * Returns the identifier of the device.
     * Must not throw exceptions.
     */
    fun getDeviceId(): DeviceId
}

/**
 * A device id provider implementation that returns a random UUIDv4.
 */
class RandomIdProvider : DeviceIdProvider {
    override fun getDeviceId(): DeviceId {
        return UUID.randomUUID()
    }
}

/**
 * A device id provider implementation that uses a persistent cache for storing and retrieving
 * device identifiers generated by a base device id provider.
 *
 * By default it uses a random id provider for generating a new id in case of a cache miss.
 */
class CachingIdProvider(
        private val baseProvider: DeviceIdProvider = RandomIdProvider()
) : DeviceIdProvider {

    var cacheService: CacheService? = null

    private val cacheKey = "speechly-device-id"

    override fun getDeviceId(): DeviceId {
        return this.loadFromCache() ?: this.storeAndReturn()
    }

    private fun loadFromCache(): DeviceId? {
        val cached = this.cacheService?.loadString(this.cacheKey) ?: return null

        return try {
            UUID.fromString(cached)
        } catch (_: Throwable) {
            null
        }
    }

    private fun storeAndReturn(): DeviceId {
        val id = this.baseProvider.getDeviceId()

        // `storeString` returns false if the write operation has failed.
        // Current we choose to ignore failed writes and instead re-generate the id on the next call.
        this.cacheService?.storeString(this.cacheKey, id.toString())

        return id
    }
}