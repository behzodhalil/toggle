package io.behzodhalil.togglecore.resolver

import io.behzodhalil.togglecore.core.FeatureFlag
import io.behzodhalil.togglecore.core.FeatureKey
import io.behzodhalil.togglecore.context.ToggleContext
import io.behzodhalil.togglecore.evaluator.NoOpRuleEvaluator
import io.behzodhalil.togglecore.evaluator.RuleEvaluator
import io.behzodhalil.togglecore.logger.ToggleLogger
import io.behzodhalil.togglecore.source.FeatureSource
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList


/**
 * Resolves feature flags by querying sources and applying evaluation rules.
 *
 * The resolver implements a multi-stage pipeline:
 *
 * ```
 * Feature Key → Source Resolution → Rule Evaluation → Flag
 * ```
 *
 * 1. **Source Resolution**: Query sources in priority order until flag found
 * 2. **Rule Evaluation**: Apply contextual rules (targeting, rollouts, etc.)
 * 3. **Fallback**: Return disabled flag if no source provides the feature
 *
 * All implementations must be thread-safe for concurrent resolution.
 * The default implementation achieves this through:
 * - Immutable source list
 * - Stateless evaluation
 * - No shared mutable state
 * @see DefaultFeatureResolver
 */
public interface FeatureResolver {
    /**
     * Context for feature evaluation.
     *
     * Provides user, environment, and custom attributes for targeting rules.
     * Context should be immutable or effectively immutable.
     */
    public val context: ToggleContext

    /**
     * Resolves a feature flag by key.
     */
    public fun resolve(key: String): FeatureFlag

    /**
     * Resolves a feature flag using a type-safe key.
     *
     * Convenience method that delegates to [resolve] with the key's value.
     *
     * @param key Type-safe feature key
     * @return The resolved feature flag
     */
    public fun resolve(key: FeatureKey): FeatureFlag = resolve(key.value)

    /**
     * Batch resolves multiple feature flags.
     *
     * More efficient than calling [resolve] multiple times as it can
     * optimize source queries.
     *
     * Default implementation resolves individually - override for optimization.
     *
     * @param keys Feature keys to resolve
     * @return Map of keys to resolved flags
     */
    public fun resolveAll(keys: Set<String>): Map<String, FeatureFlag> {
        return keys.associateWith { resolve(it) }
    }
}

/**
 * Default implementation of [FeatureResolver] with priority-based source resolution.
 *
 * Sources are queried in descending priority order. The first source to return
 * a non-null flag wins. After resolution, the [RuleEvaluator] applies contextual
 * rules to produce the final flag state.
 *
 *
 * ### Example
 * ```kotlin
 * val resolver = DefaultFeatureResolver(
 *     sources = listOf(
 *         MemorySource(),          // Priority 200
 *         YamlSource.fromString(), // Priority 120
 *         RemoteConfigSource()     // Priority 80
 *     ).sortedByDescending { it.priority },
 *     evaluator = PercentageRolloutEvaluator(50),
 *     context = ToggleContext(userId = "user_123"),
 *     logger = ToggleLogger()
 * )
 * ```
 *
 * @property sources Immutable list of sources, pre-sorted by priority
 * @property evaluator Rule evaluator for contextual logic
 * @property context Evaluation context
 * @property logger Optional logger for observability
 */
public class DefaultFeatureResolver(
    sources: List<FeatureSource>,
    private val evaluator: RuleEvaluator = NoOpRuleEvaluator.INSTANCE,
    override val context: ToggleContext,
    private val logger: ToggleLogger? = null,
) : FeatureResolver {

    /**
     * Immutable, pre-sorted source list for thread-safe concurrent access.
     */
    private val sources: ImmutableList<FeatureSource> = sources.toImmutableList()

    init {
        require(sources.isNotEmpty()) {
            "DefaultFeatureResolver requires at least one source"
        }
    }


    override fun resolve(key: String): FeatureFlag {
        require(key.isNotBlank()) { "Feature key cannot be blank" }

        val sourceFlag = resolveFromSources(key)

        return evaluateWithFailure(sourceFlag, key)
    }

    override fun resolveAll(keys: Set<String>): Map<String, FeatureFlag> {
        return keys.associateWith { key ->
            resolve(key)
        }
    }

    /**
     * Queries sources in priority order until a flag is found.
     *
     * Uses sequence for lazy evaluation - only queries until first match.
     *
     * @param key Feature key to resolve
     * @return Feature flag from source, or disabled default
     */
    private fun resolveFromSources(key: String): FeatureFlag {
        return sources.asSequence()
            .mapNotNull { source ->
                resolveFromSource(source, key)
            }
            .firstOrNull()
            ?: createDefaultFlag(key)
    }

    /**
     * Safely queries a single source with error handling.
     *
     * @param source Source to query
     * @param key Feature key
     * @return Flag if found, null if not found or error occurred
     */
    private fun resolveFromSource(source: FeatureSource, key: String): FeatureFlag? {
        return try {
            val flag = source.get(key)
            if (flag != null) {
                logger?.log(
                    "Resolved '$key' from source '${source.sourceName}': enabled=${flag.enabled}"
                )
            }
            flag
        } catch (e: Exception) {
            logger?.log(
                "Source '${source.sourceName}' failed for key '$key': ${e.message}",
                e
            )
            null
        }
    }

    /**
     * Applies evaluation rules with error handling.
     *
     * If evaluator throws, falls back to source flag to maintain availability.
     *
     * @param flag Source flag to evaluate
     * @param key Feature key (for error logging)
     * @return Evaluated flag, or source flag if evaluation fails
     */
    private fun evaluateWithFailure(flag: FeatureFlag, key: String): FeatureFlag {
        return try {
            val evaluated = evaluator.evaluate(flag, context)

            // Log if evaluation changed the flag
            if (evaluated.enabled != flag.enabled) {
                logger?.log(
                    "Evaluation changed '$key': ${flag.enabled} → ${evaluated.enabled}"
                )
            }

            evaluated
        } catch (e: Exception) {
            logger?.log(
                "Evaluator failed for key '$key', using source flag: ${e.message}",
                e
            )
            flag
        }
    }

    /**
     * Creates default disabled flag when no source provides the feature.
     *
     * @param key Feature key
     * @return Disabled feature flag with default source
     */
    private fun createDefaultFlag(key: String): FeatureFlag {
        logger?.log("No source provided '$key', using disabled default")
        return FeatureFlag.disabled(key, source = "default")
    }
}