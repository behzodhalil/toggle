package io.behzodhalil.togglecore.evaluator

import io.behzodhalil.togglecore.context.ToggleContext
import io.behzodhalil.togglecore.core.FeatureFlag

/**
 * Chains multiple evaluators, applying them in sequence.
 *
 * Each evaluator receives the output of the previous one, enabling
 * complex multi-stage evaluation logic.
 *
 * ### Execution Order
 * ```
 * input → evaluator[0] → evaluator[1] → ... → evaluator[n] → output
 * ```
 *
 * ### Note
 * If any evaluator disables a flag, subsequent evaluators still run but
 * typically preserve the disabled state.
 *
 * ### Example
 * ```kotlin
 * val evaluator = CompositeRuleEvaluator(
 *     EnvironmentEvaluator(),      // Check environment
 *     UserTargetingEvaluator(),    // Check user targeting
 *     PercentageRolloutEvaluator() // Apply rollout percentage
 * )
 * ```
 *
 * @property evaluators List of evaluators to apply in order
 */
public class CompositeRuleEvaluator(
    private val evaluators: List<RuleEvaluator>,
) : RuleEvaluator {
    public constructor(vararg evaluators: RuleEvaluator) : this(evaluators.toList())

    init {
        require(evaluators.isNotEmpty()) {
            "CompositeRuleEvaluator requires at least one evaluator"
        }
    }

    override fun evaluate(
        flag: FeatureFlag,
        context: ToggleContext,
    ): FeatureFlag {
        return evaluators.fold(flag) { currentFlag, evaluator ->
            evaluator.evaluate(currentFlag, context)
        }
    }

    public companion object {
        /**
         * Creates a composite evaluator from a list, optimizing for single evaluators.
         *
         * @return The single evaluator if list contains one, otherwise a composite
         */
        public fun of(evaluators: List<RuleEvaluator>): RuleEvaluator {
            return when (evaluators.size) {
                0 -> NoOpRuleEvaluator.INSTANCE
                1 -> evaluators[0]
                else -> CompositeRuleEvaluator(evaluators)
            }
        }
    }
}
