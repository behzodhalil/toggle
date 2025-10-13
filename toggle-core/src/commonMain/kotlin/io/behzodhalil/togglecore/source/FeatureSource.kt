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
public interface FeatureSource : AutoCloseable {

    /**
     * Source identifier for logging and debugging.
     *
     * Examples: "memory", "yaml-config", "firebase-remote-config"
     */
    public val sourceName: String

    /**
     * Priority for source ordering. Higher = first.
     */
    public val priority: Int get() = DEFAULT_PRIORITY


    /**
     * Get feature by key.
     */
    public fun get(key: String): FeatureFlag?

    /**
     * Reload source data.
     */
    public suspend fun refresh(): Unit = Unit

    /**
     * Retrieve all features from this source.
     */
    public fun getAll(): List<FeatureFlag> = emptyList()

    override fun close(): Unit = Unit

    public companion object {
        public const val DEFAULT_PRIORITY: Int = 0
    }
}