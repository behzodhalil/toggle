package io.behzodhalil.togglecore.evaluator

import io.behzodhalil.togglecore.context.ToggleContext
import io.behzodhalil.togglecore.core.FeatureFlag
import io.behzodhalil.togglecore.core.ToggleInternal

@ToggleInternal
public class RuleEvaluationScope internal constructor() {
    private val evaluators = mutableListOf<RuleEvaluator>()

    /**
     * Add a raw evaluator instance.
     */
    public fun evaluator(evaluator: RuleEvaluator) {
        evaluators.add(evaluator)
    }

    /**
     * Create a conditional rule builder.
     */
    public fun whenFlag(predicate: (FeatureFlag, ToggleContext) -> Boolean): ConditionalBuilder {
        return ConditionalBuilder(predicate)
    }

    public inner class ConditionalBuilder(
        private val predicate: (FeatureFlag, ToggleContext) -> Boolean,
    ) {
        public infix fun then(evaluator: RuleEvaluator) {
            evaluators.add(ConditionalRuleEvaluator(predicate, evaluator))
        }

        public infix fun then(block: RuleEvaluationScope.() -> Unit) {
            val builder = RuleEvaluationScope()
            builder.block()
            evaluators.add(ConditionalRuleEvaluator(predicate, builder.build()))
        }
    }

    internal fun build(): RuleEvaluator {
        return CompositeRuleEvaluator.of(evaluators)
    }
}
