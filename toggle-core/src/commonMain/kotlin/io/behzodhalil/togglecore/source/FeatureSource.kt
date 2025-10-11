package io.behzodhalil.togglecore.source

import io.behzodhalil.togglecore.core.FeatureFlag

/**
 * Source of feature flag data.
 *
 * ```kotlin
 * class RemoteSource : FeatureSource {
 *     override val priority = 100
 *     override val sourceName = "memory
 *     override fun getFeature(key: String) = remoteFlags[key]
 * }
 * ```
 */
interface FeatureSource : AutoCloseable {

    /**
     * Source identifier for logging and debugging.
     *
     * Examples: "memory", "yaml-config", "firebase-remote-config"
     */
    val sourceName: String

    /**
     * Priority for source ordering. Higher = first.
     */
    val priority: Int get() = DEFAULT_PRIORITY


    /**
     * Get feature by key.
     */
    fun get(key: String): FeatureFlag?

    /**
     * Reload source data.
     */
    suspend fun refresh(): Unit = Unit

    /**
     * Retrieve all features from this source.
     */
    fun getAll(): List<FeatureFlag> = emptyList()

    override fun close() = Unit

    companion object {
        const val DEFAULT_PRIORITY = 0
    }
}