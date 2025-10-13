package io.behzodhalil.togglecore.resolver

import io.behzodhalil.togglecore.core.FeatureFlag
import io.behzodhalil.togglecore.context.ToggleContext
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.collections.immutable.persistentMapOf

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
public class CachingFeatureResolver(
    private val delegate: FeatureResolver,
) : FeatureResolver {

    private val cache = atomic(persistentMapOf<String, FeatureFlag>())
    private val lock = reentrantLock()

    override val context: ToggleContext
        get() = delegate.context

    override fun resolve(key: String): FeatureFlag {
        val cached = cache.value[key]
        if (cached != null) return cached

        return lock.withLock {
            val newFlag = cache.value[key]
            if (newFlag != null) return newFlag

            val resolved = delegate.resolve(key)
            cache.value = cache.value.put(key, resolved)
            resolved
        }
    }

    override fun resolveAll(keys: Set<String>): Map<String, FeatureFlag> {
        val currentCache = cache.value
        val uncached = keys.filterNot { it in currentCache }.toSet()

        if (uncached.isEmpty()) {
            return keys.associateWith { currentCache[it]!! }
        }

        return lock.withLock {
            val resolved = delegate.resolveAll(uncached)
            cache.value = cache.value.putAll(resolved)

            keys.associateWith { cache.value[it]!! }
        }
    }
    /**
     * Invalidates cached result for a specific key.
     *
     * @param key Feature key to invalidate
     */
    public fun invalidate(key: String) {
        cache.value.remove(key)
    }

    /**
     * Invalidates all cached results.
     */
    public fun invalidateAll() {
        cache.value.clear()
    }
}