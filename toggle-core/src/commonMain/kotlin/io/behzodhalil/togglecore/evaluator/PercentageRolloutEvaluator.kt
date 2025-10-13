package io.behzodhalil.togglecore.evaluator

import io.behzodhalil.togglecore.context.ToggleContext
import io.behzodhalil.togglecore.core.FeatureFlag
import kotlin.math.abs

public class PercentageRolloutEvaluator : RuleEvaluator {
    override fun evaluate(flag: FeatureFlag, context: ToggleContext): FeatureFlag {
        if (!flag.enabled) return flag

        val rolloutPercentage = flag.metadata["rollout"]?.removeSuffix("%")?.toIntOrNull()
            ?: return flag

        val userId = context.userId
            ?: return flag.withEnabled(false)

        val bucket = abs(userId.hashCode()) % 100

        val isEnabled = bucket < rolloutPercentage
        return flag.withEnabled(isEnabled)
    }
}
