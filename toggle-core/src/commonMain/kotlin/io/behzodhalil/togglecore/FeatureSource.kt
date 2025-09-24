package io.behzodhalil.togglecore


/**
 * Source of feature flag data.
 *
 * ```kotlin
 * class RemoteSource : FeatureSource {
 *     override val priority = 100
 *     override fun getFeature(key: String) = remoteFlags[key]
 * }
 * ```
 */
interface FeatureSource {
    /** Get feature by key. */
    fun value(key: String): FeatureFlag?

    /** Reload source data. */
    suspend fun refresh(): Unit = Unit

    /** Get all features. */
    fun values(): List<FeatureFlag> = emptyList()

    /** Priority for source ordering. Higher = first. */
    val priority: Int get() = 0
}
