package io.behzodhalil.togglecore


/**
 * Resolver that queries sources in priority order.
 *
 * Sources are tried sequentially until one returns a flag. If all fail,
 * returns disabled flag.
 *
 * ```kotlin
 * val resolver = DefaultFeatureResolver(
 *     sources = listOf(remoteSource, localSource),
 *     evaluator = evaluator,
 *     logger = logger,
 *     context = context
 * )
 * ```
 */
interface FeatureResolver {
    /**
     * Context for feature evaluation.
     */
    val context: ToggleContext

    /**
     * Get feature flag for key.
     * Returns disabled if not found.
     */
    fun resolve(key: String): FeatureFlag
}


class DefaultFeatureResolver(
    private val sources: List<FeatureSource>,
    private val evaluator: FeatureEvaluator,
    private val logger: ToggleLogger?,
    override val context: ToggleContext,
) : FeatureResolver {

    override fun resolve(key: String): FeatureFlag {
        val flag = sources.asSequence()
            .mapNotNull { source ->
                try {
                    source.value(key)
                } catch (e: Exception) {
                    logger?.log(
                        "Source ${source::class.simpleName} failed for $key: ${e.message}",
                        e
                    )
                    null
                }
            }
            .firstOrNull() ?: FeatureFlag.disabled(key)

        return evaluator.evaluate(flag, context)
    }
}