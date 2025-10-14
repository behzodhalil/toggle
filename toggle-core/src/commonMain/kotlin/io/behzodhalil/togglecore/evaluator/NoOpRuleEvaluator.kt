package io.behzodhalil.togglecore.evaluator

import io.behzodhalil.togglecore.context.ToggleContext
import io.behzodhalil.togglecore.core.FeatureFlag

/**
 * Pass-through evaluator that returns flags unchanged.
 *
 * Used as the default when no custom evaluation logic is needed.
 * Zero overhead - simply returns the input flag.
 *
 */
internal class NoOpRuleEvaluator private constructor() : RuleEvaluator {
    override fun evaluate(
        flag: FeatureFlag,
        context: ToggleContext,
    ): FeatureFlag {
        return flag
    }

    companion object {
        val INSTANCE = NoOpRuleEvaluator()
    }
}
