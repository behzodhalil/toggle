package io.behzodhalil.togglecore.evaluator

import io.behzodhalil.togglecore.core.FeatureFlag
import io.behzodhalil.togglecore.context.ToggleContext

/**
 * Evaluates feature flags against contextual rules to enable dynamic feature targeting.
 *

 */
fun interface RuleEvaluator {
    /**
     * Evaluates a feature flag in the given context.
     *
     * Thread Safety
     *
     * This method may be called concurrently from multiple threads. Implementations
     * must be thread-safe or document synchronization requirements.
     *
     * Performance
     *
     * This is called on the hot path for every feature check. Keep evaluation fast:
     * - Avoid I/O operations
     * - Minimize allocations
     * - Cache expensive computations
     *
     * @param flag The feature flag to evaluate
     * @param context The evaluation context (user, environment, etc.)
     * @return The evaluated feature flag (may be the same instance if no changes)
     */
    fun evaluate(flag: FeatureFlag, context: ToggleContext): FeatureFlag
}