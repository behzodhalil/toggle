package io.behzodhalil.togglecore.core

import io.behzodhalil.togglecore.context.ToggleContext
import io.behzodhalil.togglecore.error.RefreshException
import io.behzodhalil.togglecore.logger.ToggleLogger
import io.behzodhalil.togglecore.resolver.CachingFeatureResolver
import io.behzodhalil.togglecore.resolver.FeatureResolver
import io.behzodhalil.togglecore.source.FeatureSource
import kotlinx.atomicfu.atomic
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds

/**
 * Provides a unified interface for evaluating feature flag states and managing feature flag configurations.
 *
 * Implements a layered resolver pattern with priority-based source composition:
 *
 * ```
 * ┌─────────────────────────────────────┐
 * │           Toggle                    │
 * ├─────────────────────────────────────┤
 * │      CachingFeatureResolver         │
 * ├─────────────────────────────────────┤
 * │   FeatureResolver + RuleEvaluator   │
 * ├─────────────────────────────────────┤
 * │    FeatureSource (priority-based)   │
 * └─────────────────────────────────────┘
 * ```
 *
 * ### Source Priority
 *
 * Sources are queried in descending priority order:
 * - **Runtime overrides** (Memory): 200
 * - **Local configuration** (YAML): 120
 * - **Remote services**: 80
 *
 * ### Usage Example
 * ```
 * // Create Toggle instance
 * val toggle = Toggle {
 *     sources {
 *         memory {
 *             feature(FeatureKey.EXPERIMENTAL_API, true)
 *         }
 *         yaml {
 *             content = """
 *                 features:
 *                   experimental_api: true
 *             """.trimIndent()
 *         }
 *     }
 *
 *     context {
 *         userId("user_123")
 *         country("US")
 *         appVersion("2.0.0")
 *     }
 *
 *     evaluation {
 *         percentageRollout(50)
 *     }
 *
 *     debug() // Enable logging
 * }
 *
 * // Check features
 * if (toggle.isEnabled(FeatureKey.EXPERIMENTAL_API)) {
 *     applyExperimentalApi()
 * }
 *
 * // Get full flag with metadata
 * val flag = toggle.value(FeatureKey.BETA_UI)
 * println("Source: ${flag.source}, Bucket: ${flag.metadata["rollout_bucket"]}")
 *
 * // Refresh from sources
 * scope.launch {
 *     toggle.refresh()
 *         .onSuccess { println("Refreshed successfully") }
 *         .onFailure { println("Refresh failed: $it") }
 * }
 *
 * // Cleanup when done
 * toggle.close()
 * ```
 *
 * ### Lifecycle States
 *
 * ```
 * CREATED → ACTIVE → DISPOSED
 *    ↓         ↓         ↓
 *  ready   evaluating  error
 * ```
 *
 * @see FeatureResolver
 * @see FeatureSource
 * @see FeatureKey
 */
public class Toggle private constructor(
    private val resolver: FeatureResolver,
    private val cachingResolver: CachingFeatureResolver?,
    private val sources: ImmutableList<FeatureSource>,
    private val logger: ToggleLogger?,
    private val scope: CoroutineScope,
) : AutoCloseable {
    /**
     * Lifecycle state using atomic for thread-safe transitions.
     */
    private val state = atomic(State.ACTIVE)

    /**
     * Evaluation context for targeting rules.
     *
     * Immutable after construction. To change context, create a new Toggle instance.
     */
    public val context: ToggleContext
        get() = resolver.context

    /**
     * Checks if a feature is enabled.
     *
     * ## Example
     * ```kotlin
     * when {
     *     toggle.isEnabled(Features.PREMIUM) -> showPremiumFeature()
     *     toggle.isEnabled(Features.BETA_UI) -> showBetaUI()
     *     else -> showDefaultUI()
     * }
     * ```
     *
     * @param feature Type-safe feature key
     * @return `true` if enabled, `false` otherwise (never throws)
     */
    public fun isEnabled(feature: FeatureKey): Boolean {
        if (!isActive()) return false

        return runCatching {
            value(feature).enabled
        }.getOrElse { error ->
            logger?.log("Error checking feature '${feature.value}': ${error.message}", error)
            false
        }
    }

    /**
     * Checks if a feature is enabled by string key.
     *
     * Less type-safe than [isEnabled] with [FeatureKey]. Prefer the type-safe variant.
     *
     * @param key Feature key string
     * @return `true` if enabled, `false` otherwise
     */
    public fun isEnabled(key: String): Boolean {
        if (!isActive()) return false

        return runCatching {
            value(key).enabled
        }.getOrElse { error ->
            logger?.log("Error checking feature '$key': ${error.message}", error)
            false
        }
    }

    /**
     * Gets the complete feature flag with metadata.
     *
     * Use this when you need:
     * - Source information (where the flag came from)
     * - Metadata (rollout bucket, targeting info, owner, etc.)
     * - Timestamp (when the flag was created)
     *
     * ### Example
     * ```kotlin
     * val flag = toggle.value(FeatureKey.of("EXPERIMENT"))
     *
     * if (flag.enabled) {
     *     val source = flag.source
     *     val variant = flag.metadata["variant"] ?: "control"
     *     val bucket = flag.metadata["rollout_bucket"]?.toInt() ?: 0
     *
     *     analytics.track("experiment_shown", mapOf(
     *         "source" to source,
     *         "variant" to variant,
     *         "bucket" to bucket
     *     ))
     * }
     * ```
     *
     * @param key Type-safe feature key
     * @return Feature flag (never null, never throws)
     */
    public fun value(key: FeatureKey): FeatureFlag {
        return value(key.value)
    }

    /**
     * Gets the complete feature flag by string key.
     *
     * @param key Feature key string
     * @return Feature flag (never null, never throws)
     */
    public fun value(key: String): FeatureFlag {
        if (!isActive()) {
            return FeatureFlag.disabled(key, source = "disposed")
        }

        return runCatching {
            require(key.isNotBlank()) { "Feature key cannot be blank" }
            resolver.resolve(key)
        }.getOrElse { error ->
            logger?.log("Failed to resolve feature '$key': ${error.message}", error)
            FeatureFlag.disabled(key, source = "error")
        }
    }

    /**
     * Gets all registered features with their current values.
     *
     * ### Use Cases
     * - Admin/debug UI showing all features
     * - Feature auditing and compliance
     * - Testing and validation
     * - Documentation generation
     *
     * ### Example
     * ```kotlin
     * // Debug panel
     * fun showFeatureDebugPanel() {
     *     val features = toggle.values()
     *     features.forEach { (key, flag) ->
     *         println("$key: enabled=${flag.enabled}, source=${flag.source}")
     *     }
     * }
     * ```
     *
     * @return Immutable map of feature key to flag
     */
    public fun values(): Map<String, FeatureFlag> {
        if (!isActive()) {
            return emptyMap()
        }

        return runCatching {
            FeatureKey.registry.associate { feature ->
                feature.value to value(feature)
            }
        }.getOrElse { error ->
            logger?.log("Failed to get all features: ${error.message}", error)
            emptyMap()
        }
    }

    /**
     * Refreshes all sources and clears the cache.
     *
     * ### Process
     * 1. Clear resolution cache
     * 2. Call [FeatureSource.refresh] on each source in parallel
     * 3. Collect and log results
     *
     * @return [Result.success] if at least one source refreshed, [Result.failure] if all failed
     */
    public suspend fun refresh(): Result<Unit> =
        runCatching {
            checkActive()

            withContext(Dispatchers.Default) {
                logger?.log("Starting refresh of ${sources.size} source(s)")

                cachingResolver?.invalidateAll()

                // Parallel refresh with timeout
                val results =
                    sources.map { source ->
                        async {
                            try {
                                withTimeout(30.seconds) {
                                    source.refresh()
                                    SourceRefreshResult.Success(source.sourceName)
                                }
                            } catch (e: TimeoutCancellationException) {
                                SourceRefreshResult.Failure(
                                    source.sourceName,
                                    e,
                                )
                            } catch (e: Exception) {
                                SourceRefreshResult.Failure(source.sourceName, e)
                            }
                        }
                    }.awaitAll()

                // Log results
                results.forEach { result ->
                    when (result) {
                        is SourceRefreshResult.Success ->
                            logger?.log("✓ Refreshed source: ${result.sourceName}")
                        is SourceRefreshResult.Failure ->
                            logger?.log("✗ Failed: '${result.sourceName}': ${result.error.message}", result.error)
                    }
                }

                val failures = results.filterIsInstance<SourceRefreshResult.Failure>()
                if (failures.size == sources.size && failures.isNotEmpty()) {
                    throw RefreshException(
                        message = "All ${sources.size} source(s) failed to refresh",
                        failures = failures.map { it.error },
                    )
                }

                val successCount = results.size - failures.size
                logger?.log("Refresh complete: $successCount/${results.size} source(s) succeeded")
            }
        }

    /**
     * Invalidates the cached result for a specific feature.
     *
     * Next call to [value] or [isEnabled] will re-query sources and re-evaluate rules.
     *
     * ### Use Cases
     * - Source changed for a specific feature
     * - Testing different values
     * - Manual cache invalidation
     * - Debugging evaluation issues
     *
     * ### Example
     * ```kotlin
     * // Update feature in memory source
     * memorySource.setFeature(Features.BETA, true)
     *
     * // Invalidate cache to pick up change
     * toggle.invalidate(Features.BETA)
     *
     * // Next call will re-evaluate
     * val isBeta = toggle.isEnabled(Features.BETA) // true
     * ```
     *
     * @param key Feature key to invalidate
     */
    public fun invalidate(key: FeatureKey) {
        invalidate(key.value)
    }

    /**
     * Invalidates the cached result for a specific feature by string key.
     *
     * @param key Feature key string to invalidate
     */
    public fun invalidate(key: String) {
        if (!isActive()) return

        cachingResolver?.invalidate(key)
        logger?.log("Invalidated cache for feature: $key")
    }

    /**
     * Invalidates all cached feature results.
     *
     * More efficient than calling [invalidate] for each feature individually.
     *
     * @see invalidate
     */
    public fun invalidateAll() {
        if (!isActive()) return

        cachingResolver?.invalidateAll()
        logger?.log("Invalidated all cached features")
    }

    /**
     * Checks if Toggle is active and ready to evaluate features.
     *
     * @return `true` if active, `false` if disposed
     */
    public fun isActive(): Boolean = state.value == State.ACTIVE

    /**
     * Checks if Toggle has been disposed.
     *
     * @return `true` if disposed, `false` if active
     */
    public fun isDisposed(): Boolean = state.value == State.DISPOSED

    /**
     * Disposes Toggle and releases all resources.
     *
     */
    override fun close() {
        // Atomic state transition
        val wasActive = state.compareAndSet(State.ACTIVE, State.DISPOSED)
        if (!wasActive) {
            logger?.log("Toggle already disposed")
            return
        }

        logger?.log("Disposing Toggle...")

        // Clear cache
        runCatching {
            cachingResolver?.invalidateAll()
        }.onFailure { error ->
            logger?.log("Error clearing cache during disposal: ${error.message}", error)
        }

        // Close sources
        sources.forEach { source ->
            runCatching {
                source.close()
                logger?.log("Closed source: ${source.sourceName}")
            }.onFailure { error ->
                logger?.log(
                    "Failed to close source '${source.sourceName}': ${error.message}",
                    error,
                )
            }
        }

        // Cancel coroutine scope
        runCatching {
            scope.cancel()
        }.onFailure { error ->
            logger?.log("Error cancelling scope: ${error.message}", error)
        }

        logger?.log("Toggle disposed successfully")
    }

    private fun checkActive() {
        check(isActive()) {
            "Toggle has been disposed. Create a new instance to continue using feature flags."
        }
    }

    /**
     * Lifecycle state for Toggle.
     */
    private enum class State {
        /** Toggle is active and ready for feature evaluation. */
        ACTIVE,

        /** Toggle has been disposed and cannot be used. */
        DISPOSED,
    }

    /**
     * Result of refreshing a single source.
     */
    private sealed interface SourceRefreshResult {
        data class Success(val sourceName: String) : SourceRefreshResult

        data class Failure(val sourceName: String, val error: Exception) : SourceRefreshResult
    }

    public companion object {
        /**
         * Creates a Toggle instance.
         *
         *
         * @param resolver Feature resolver (should be wrapped in [CachingFeatureResolver])
         * @param sources Immutable list of feature sources
         * @param logger Optional logger for observability
         * @param scope Coroutine scope for async operations
         * @return Configured Toggle instance
         */
        internal fun create(
            resolver: FeatureResolver,
            sources: List<FeatureSource>,
            logger: ToggleLogger?,
            scope: CoroutineScope,
        ): Toggle {
            // Extract caching resolver for invalidation support
            val cachingResolver = resolver as? CachingFeatureResolver

            return Toggle(
                resolver = resolver,
                cachingResolver = cachingResolver,
                sources = sources.toImmutableList(),
                logger = logger,
                scope = scope,
            )
        }
    }
}
