package io.behzodhalil.togglecore.source

import io.behzodhalil.togglecore.core.FeatureFlag
import io.behzodhalil.togglecore.error.YamlParseException
import io.behzodhalil.togglecore.util.getResourceReader
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentMap

/**
 * YAML-based feature source.
 *
 * ### Simple Format (boolean flags):
 * ```yaml
 * features:
 *   dark_mode: true
 *   new_ui: false
 *   beta_features: on
 * ```
 *
 * ### Complex Format (flags with metadata):
 * ```yaml
 * features:
 *   premium_feature:
 *     enabled: true
 *     description: "Premium user feature"
 *     metadata:
 *       owner: "team-growth"
 *       rollout: "100%"
 *       since: "2024-01-15"
 * ```
 *
 *
 * ### Example
 *
 * ```kotlin
 * // From string content
 * val source = YamlSource.fromString("""
 *     features:
 *       dark_mode: true
 *       experimental_ui: false
 * """.trimIndent(), priority = 120)
 *
 * // Access features
 * val flag = source.get("dark_mode")
 * val allFlags = source.getAll()
 *
 * // Refresh (re-parse)
 * source.refresh()
 * ```
 *
 * @property priority Source resolution priority (higher values evaluated first, default: 120)
 * @property sourceName Identifier for logging and debugging (default: "yaml_source")
 *
 * @see FeatureSource
 * @see YamlParseException
 *
 */
public class YamlSource private constructor(
    private val yamlContent: String,
    override val priority: Int = DEFAULT_PRIORITY,
) : FeatureSource {

    /**
     * Thread-safe cache of parsed features using persistent (immutable) map.
     *
     * AtomicRef ensures atomic updates with proper memory ordering.
     * PersistentMap provides structural sharing and zero-copy reads.
     */
    private val featuresCache = atomic(persistentMapOf<String, FeatureFlag>())

    /**
     * Initialization state flag.
     */
    private val initialized = atomic(false)

    /**
     * Reentrant lock for synchronizing initialization and refresh operations.
     */
    private val lock = reentrantLock()

    override val sourceName: String = DEFAULT_SOURCE_NAME

    init {
        require(yamlContent.isNotBlank()) {
            "YAML content cannot be blank"
        }
    }

    /**
     * Retrieves a feature flag by its key.
     *
     * Triggers lazy initialization on first access.
     *
     * Reads are lock-free after initialization.
     *
     * @param key Feature flag identifier
     * @return Feature flag if found, null otherwise
     *
     * @throws YamlParseException if lazy parsing fails on first access
     */
    override fun get(key: String): FeatureFlag? {
        ensureInitialized()
        return featuresCache.value[key]
    }

    /**
     * Retrieves all feature flags from this source.
     *
     * Triggers lazy initialization on first access.
     *
     * @return List of all feature flags (immutable snapshot)
     *
     * @throws YamlParseException if lazy parsing fails on first access
     */
    override fun getAll(): List<FeatureFlag> {
        ensureInitialized()
        return featuresCache.value.values.toList()
    }

    /**
     * Re-parses the YAML content and updates the cache.
     *
     * **Use Case**: Call when YAML content needs to be re-evaluated (e.g., after dynamic update).
     *
     * @throws YamlParseException if parsing fails
     */
    override suspend fun refresh() {
        parseAndCache()
    }

    /**
     * Ensures YAML is parsed before first access using double-checked locking.
     *
     * @throws YamlParseException if parsing fails
     */
    private fun ensureInitialized() {
        if (!initialized.value) {
            // Slow path: acquire lock and re-check
            lock.withLock {
                if (!initialized.value) {
                    parseAndCache()
                }
            }
        }
    }

    /**
     * Parses YAML content and atomically updates the cache.
     *
     * @throws YamlParseException if parsing fails
     */
    private fun parseAndCache() {
        try {
            val parser = YamlFeatureParser(yamlContent)
            val parsed = parser.parse()

            // Atomic update: new persistent map
            featuresCache.value = parsed.toPersistentMap()
            initialized.value = true
        } catch (e: YamlParseException) {
            initialized.value = false
            throw e
        } catch (e: Exception) {
            initialized.value = false
            throw YamlParseException(
                message = "Failed to parse YAML content",
                cause = e,
                line = extractLineNumberFromException(e)
            )
        }
    }


    public companion object {
        private const val DEFAULT_PRIORITY = 120
        private const val DEFAULT_SOURCE_NAME = "yaml_source"

        /**
         * Creates YamlSource from string content.
         *
         * @param content YAML string content
         * @param priority Source priority (default: 120)
         * @throws YamlParseException if content is invalid
         */
        public fun fromString(
            content: String,
            priority: Int = DEFAULT_PRIORITY,
        ): YamlSource {
            return YamlSource(content, priority)
        }


        /**
         *  Creates YamlSource from resource.
         *
         *  @param resourcePath Path of resource.
         *  @param priority Source priority (default: 120)
         *  @throws YamlParseException if content is invalid
         */
        public fun fromResource(
            resourcePath: String,
            priority: Int = DEFAULT_PRIORITY,
        ): YamlSource {
            val reader = getResourceReader()

            val content = reader.readText(resourcePath)

            return YamlSource(content, priority)
        }

    }
}

