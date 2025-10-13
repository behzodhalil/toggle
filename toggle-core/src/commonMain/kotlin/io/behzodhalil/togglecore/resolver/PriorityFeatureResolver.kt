package io.behzodhalil.togglecore.resolver

import io.behzodhalil.togglecore.context.ToggleContext
import io.behzodhalil.togglecore.evaluator.NoOpRuleEvaluator
import io.behzodhalil.togglecore.evaluator.RuleEvaluator
import io.behzodhalil.togglecore.logger.ToggleLogger
import io.behzodhalil.togglecore.source.FeatureSource

/**
 * Feature resolver with explicit priority-based source ordering.
 *
 * Automatically sorts sources by priority before resolution.
 * Use this when source order is not guaranteed.
 *
 * ### Usage Example
 * ```kotlin
 * // Sources automatically sorted by priority
 * val resolver = PriorityFeatureResolver(
 *     sources = listOf(
 *         remoteConfig,  // priority = 80
 *         yamlSource,    // priority = 120
 *         memorySource   // priority = 200
 *     ),
 *     // Resolved in order: memory (200) → yaml (120) → remote (80)
 *     context = context
 * )
 * ```
 *
 * @since 1.0.0
 */
public class PriorityFeatureResolver(
    sources: List<FeatureSource>,
    evaluator: RuleEvaluator = NoOpRuleEvaluator.INSTANCE,
    context: ToggleContext,
    logger: ToggleLogger? = null,
) : FeatureResolver by DefaultFeatureResolver(
    sources = sources.sortedByDescending { it.priority },
    evaluator = evaluator,
    context = context,
    logger = logger
)
