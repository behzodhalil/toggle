package io.behzodhalil.togglecore.evaluator

import io.behzodhalil.togglecore.core.FeatureFlag
import io.behzodhalil.togglecore.context.ToggleContext

/**
 * Applies evaluation logic only when a predicate matches.
 *
 * Useful for:
 * - Environment-specific rules (dev vs prod)
 * - Feature-specific logic (apply only to certain flags)
 * - Conditional targeting
 *
 * ### Example: Environment-Specific
 * ```kotlin
 * val evaluator = ConditionalRuleEvaluator(
 *     predicate = { flag, context ->
 *         context.getStringAttribute("environment") == "production"
 *     },
 *     evaluator = StrictSecurityEvaluator()
 * )
 * ```
 *
 * ### Example: Feature-Specific
 * ```kotlin
 * val evaluator = ConditionalRuleEvaluator(
 *     predicate = { flag, _ -> flag.key.startsWith("beta_") },
 *     evaluator = BetaUserEvaluator()
 * )
 * ```
 *
 * @property predicate Determines if [evaluator] should be applied
 * @property evaluator The evaluator to apply when predicate is true
 */
class ConditionalRuleEvaluator(
    private val predicate: (FeatureFlag, ToggleContext) -> Boolean,
    private val evaluator: RuleEvaluator,
) : RuleEvaluator {

    override fun evaluate(flag: FeatureFlag, context: ToggleContext): FeatureFlag {
        return if (predicate(flag, context)) {
            evaluator.evaluate(flag, context)
        } else {
            flag
        }
    }
}