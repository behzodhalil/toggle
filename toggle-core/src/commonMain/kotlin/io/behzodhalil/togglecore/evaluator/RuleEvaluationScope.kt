package io.behzodhalil.togglecore.evaluator

import io.behzodhalil.togglecore.core.FeatureFlag
import io.behzodhalil.togglecore.context.ToggleContext
import io.behzodhalil.togglecore.core.ToggleInternal


@ToggleInternal
class RuleEvaluationScope internal constructor() {
    private val evaluators = mutableListOf<RuleEvaluator>()

    /**
     * Add a raw evaluator instance.
     */
    fun evaluator(evaluator: RuleEvaluator) {
        evaluators.add(evaluator)
    }

    /**
     * Create a conditional rule builder.
     */
    fun whenFlag(predicate: (FeatureFlag, ToggleContext) -> Boolean): ConditionalBuilder {
        return ConditionalBuilder(predicate)
    }

    inner class ConditionalBuilder(
        private val predicate: (FeatureFlag, ToggleContext) -> Boolean
    ) {
        infix fun then(evaluator: RuleEvaluator) {
            evaluators.add(ConditionalRuleEvaluator(predicate, evaluator))
        }

        infix fun then(block: RuleEvaluationScope.() -> Unit) {
            val builder = RuleEvaluationScope()
            builder.block()
            evaluators.add(ConditionalRuleEvaluator(predicate, builder.build()))
        }
    }

    internal fun build(): RuleEvaluator {
        return CompositeRuleEvaluator.of(evaluators)
    }
}