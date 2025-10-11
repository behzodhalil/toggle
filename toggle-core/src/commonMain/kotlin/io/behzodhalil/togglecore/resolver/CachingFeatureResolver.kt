package io.behzodhalil.togglecore.resolver

import io.behzodhalil.togglecore.core.FeatureFlag
import io.behzodhalil.togglecore.context.ToggleContext
import kotlinx.atomicfu.atomic

/**
 * Decorator that caches resolution results.
 *
 * Wraps another resolver and caches results per key. Useful when:
 * - Same features checked repeatedly
 * - Source queries are expensive
 * - Evaluation logic is complex
 *
 * Call [invalidate] or [invalidateAll] when sources change.
 *
 * @property delegate Underlying resolver
 * @property cache Cache storage
 */
class CachingFeatureResolver(
    private val delegate: FeatureResolver,
) : FeatureResolver {

    private val cache = atomic<MutableMap<String, FeatureFlag>>(mutableMapOf())

    override val context: ToggleContext
        get() = delegate.context

    override fun resolve(key: String): FeatureFlag {
        return cache.value.getOrPut(key) {
            delegate.resolve(key)
        }
    }

    override fun resolveAll(keys: Set<String>): Map<String, FeatureFlag> {
        val result = mutableMapOf<String, FeatureFlag>()
        val uncached = mutableSetOf<String>()

        // Collect cached values
        keys.forEach { key ->
            val cached = cache.value[key]
            if (cached != null) {
                result[key] = cached
            } else {
                uncached.add(key)
            }
        }

        // Batch resolve uncached
        if (uncached.isNotEmpty()) {
            val resolved = delegate.resolveAll(uncached)
            cache.value.putAll(resolved)
            result.putAll(resolved)
        }

        return result
    }

    /**
     * Invalidates cached result for a specific key.
     *
     * @param key Feature key to invalidate
     */
    fun invalidate(key: String) {
        cache.value.remove(key)
    }

    /**
     * Invalidates all cached results.
     */
    fun invalidateAll() {
        cache.value.clear()
    }
}