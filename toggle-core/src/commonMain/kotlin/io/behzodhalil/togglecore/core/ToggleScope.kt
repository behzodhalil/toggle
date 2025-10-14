package io.behzodhalil.togglecore.core

import io.behzodhalil.togglecore.context.ToggleContext
import io.behzodhalil.togglecore.context.ToggleContextScope
import io.behzodhalil.togglecore.evaluator.NoOpRuleEvaluator
import io.behzodhalil.togglecore.evaluator.RuleEvaluationScope
import io.behzodhalil.togglecore.evaluator.RuleEvaluator
import io.behzodhalil.togglecore.logger.LoggingScope
import io.behzodhalil.togglecore.logger.ToggleLogger
import io.behzodhalil.togglecore.logger.createPlatformLogger
import io.behzodhalil.togglecore.resolver.CachingFeatureResolver
import io.behzodhalil.togglecore.resolver.DefaultFeatureResolver
import io.behzodhalil.togglecore.source.FeatureSource
import io.behzodhalil.togglecore.source.SourcesScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

public fun Toggle(configure: ToggleScope.() -> Unit): Toggle {
    val builder = ToggleScope()
    builder.configure()
    return builder.build()
}

@ToggleInternal
public class ToggleScope {
    private val sources = mutableListOf<FeatureSource>()
    private var logger: ToggleLogger? = null
    private var evaluator: RuleEvaluator = NoOpRuleEvaluator.INSTANCE
    private var context: ToggleContext = ToggleContext()
    private var scope: CoroutineScope? = null

    /**
     * Configure feature sources
     */
    public fun sources(configure: SourcesScope.() -> Unit) {
        val sourcesBuilder = SourcesScope()
        sourcesBuilder.configure()
        sources.addAll(sourcesBuilder.build())
    }

    /**
     * Configure context for feature evaluation
     */
    public fun context(configure: ToggleContextScope.() -> Unit) {
        val contextBuilder = ToggleContextScope()
        contextBuilder.configure()
        this.context = contextBuilder.build()
    }

    /**
     * Configure evaluation logic
     */
    public fun evaluation(configure: RuleEvaluationScope.() -> Unit) {
        val evaluationBuilder = RuleEvaluationScope()
        evaluationBuilder.configure()
        this.evaluator = evaluationBuilder.build()
    }

    /**
     * Configure logging
     */
    public fun logging(configure: LoggingScope.() -> Unit) {
        val loggingBuilder = LoggingScope()
        loggingBuilder.configure()
        this.logger = loggingBuilder.build()
    }

    /**
     * Set coroutine scope
     */
    public fun scope(scope: CoroutineScope) {
        this.scope = scope
    }

    /**
     * Enable debug logging (shorthand)
     */
    public fun debug() {
        this.logger = createPlatformLogger()
    }

    internal fun build(): Toggle {
        require(sources.isNotEmpty()) { "At least one feature source must be configured" }

        val sortedSources = sources.sortedByDescending { it.priority }
        val coroutineScope = scope ?: CoroutineScope(SupervisorJob() + Dispatchers.Default)

        // Create base resolver
        val baseResolver =
            DefaultFeatureResolver(
                sources = sortedSources,
                evaluator = evaluator,
                context = context,
                logger = logger,
            )

        // Wrap with caching for performance
        val cachingResolver = CachingFeatureResolver(baseResolver)

        return Toggle.create(
            resolver = cachingResolver,
            sources = sources.toList(),
            logger = logger,
            scope = coroutineScope,
        )
    }
}
