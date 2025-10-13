package io.behzodhalil.togglecore.evaluator

import io.behzodhalil.togglecore.core.FeatureFlag
import io.behzodhalil.togglecore.context.ToggleContext

/**
 * Evaluates feature flags against contextual rules to enable dynamic feature targeting.
 */
public fun interface RuleEvaluator {
    /**
     * Evaluates a feature flag in the given context.
     *
     * @param flag The feature flag to evaluate
     * @param context The evaluation context (user, environment, etc.)
     * @return The evaluated feature flag (may be the same instance if no changes)
     */
    public fun evaluate(flag: FeatureFlag, context: ToggleContext): FeatureFlag
}