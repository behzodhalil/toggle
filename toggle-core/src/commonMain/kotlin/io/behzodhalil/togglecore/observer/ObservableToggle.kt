package io.behzodhalil.togglecore.observer

import io.behzodhalil.togglecore.core.FeatureKey
import io.behzodhalil.togglecore.core.Toggle
import io.behzodhalil.togglecore.logger.ToggleLogger
import io.behzodhalil.togglecore.logger.createPlatformLogger
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Defines the public contract for a reactive feature toggle system.
 *
 * [ObservableToggle] extends [Toggle] with reactive observation capabilities using Kotlin Flow.
 * It provides both modern reactive patterns (StateFlow/SharedFlow) and callback-based observers
 * for maximum flexibility across different architectural styles.
 *
 * ```
 * ┌─────────────────────────────────────┐
 * │         ObservableToggle            │
 * ├─────────────────────────────────────┤
 * │  features: StateFlow<Map>           │
 * │  changes: SharedFlow<Event>         │
 * ├─────────────────────────────────────┤
 * │              Toggle                 │
 * └─────────────────────────────────────┘
 * ```
 *
 * ### Example
 *
 * ```kotlin
 * // Create with builder
 * val observable = ObservableToggle {
 *     toggle = myToggle
 *     scope = viewModelScope
 *     bufferCapacity = 128
 * }
 *
 * // Observe specific feature
 * observable.observe(Features.DARK_MODE)
 *     .onEach { enabled ->
 *         updateTheme(if (enabled) Theme.Dark else Theme.Light)
 *     }
 *     .launchIn(viewModelScope)
 *
 * // Observe all changes
 * observable.changes
 *     .filter { it.key == "premium" }
 *     .onEach { event ->
 *         analytics.track("feature_changed", mapOf(
 *             "feature" to event.key,
 *             "new_value" to event.newValue
 *         ))
 *     }
 *     .launchIn(viewModelScope)
 *
 * // Check current state (synchronous)
 * if (observable.isEnabled(Features.ANALYTICS)) {
 *     sendAnalytics()
 * }
 *
 * // Cleanup
 * observable.close()
 * ```
 *
 * @see Toggle
 * @see FeatureKey
 * @see FeatureChangeEvent
 */
public interface ObservableToggle : AutoCloseable {
    /**
     * A reactive snapshot of the enabled state for ALL registered features.
     *
     * This [StateFlow] emits a new immutable map whenever any feature changes.
     * The map contains only registered features from [FeatureKey.registry].
     *
     * ### Example
     * ```kotlin
     * observable.features
     *     .onEach { allFeatures ->
     *         val enabledCount = allFeatures.count { it.value }
     *         logger.debug("Features enabled: $enabledCount/${allFeatures.size}")
     *     }
     *     .launchIn(scope)
     * ```
     */
    public val features: StateFlow<ImmutableMap<String, Boolean>>

    /**
     * A stream of events for any feature flag state change.
     *
     * This [SharedFlow] emits [FeatureChangeEvent] whenever a feature's state changes,
     * including old value, new value, and timestamp.
     *
     * ### Example
     * ```kotlin
     * observable.changes
     *     .onEach { event ->
     *         analytics.track("feature_toggled", mapOf(
     *             "feature" to event.key,
     *             "enabled" to event.newValue,
     *             "source" to event.source,
     *             "timestamp" to event.timestamp
     *         ))
     *     }
     *     .launchIn(analyticsScope)
     * ```
     */
    public val changes: SharedFlow<FeatureChangeEvent>

    /**
     * Checks the current enabled state of a feature synchronously.
     *
     * This is a convenience method that delegates to the underlying [Toggle].
     * Use this for imperative checks where reactive observation is not needed.
     *
     * ### Example
     * ```kotlin
     * fun processRequest() {
     *     if (observable.isEnabled(Features.RATE_LIMITING)) {
     *         checkRateLimit()
     *     }
     *     // process...
     * }
     * ```
     *
     * @param feature Type-safe feature key
     * @return Current enabled state
     */
    public fun isEnabled(feature: FeatureKey): Boolean

    /**
     * Checks if a feature is enabled using string key (less type-safe).
     *
     * Prefer [isEnabled] with [FeatureKey] for type safety.
     *
     * @param key Feature key string
     * @return Current enabled state
     */
    public fun isEnabled(key: String): Boolean

    /**
     * Reactively observes a single feature's state.
     *
     * Returns a [StateFlow] that emits the current value immediately and then on every change.
     * The flow is cached per feature key for efficiency.
     *
     * ## Example
     * ```kotlin
     * // In ViewModel
     * val isDarkMode: StateFlow<Boolean> =
     *     observable.observe(Features.DARK_MODE)
     *
     * // In Compose UI
     * val isDark by isDarkMode.collectAsState()
     * MaterialTheme(
     *     colorScheme = if (isDark) darkColors else lightColors
     * ) {
     *     Content()
     * }
     * ```
     *
     * @param feature Type-safe feature key to observe
     * @return StateFlow that emits boolean state changes
     */
    public fun observe(feature: FeatureKey): StateFlow<Boolean>

    /**
     * Adds a callback-based observer for a specific feature key.
     *
     * This is a legacy-style imperative API provided for migration scenarios
     * and interop with callback-based architectures. For new code, prefer
     * [observe] with Flow-based observation.
     *
     *
     * @param featureKey Feature key string to observe
     * @param observer Callback invoked on state changes
     * @return Unsubscribe function - call to remove the observer
     */
    public fun addObserver(featureKey: String, observer: FeatureObserver): () -> Unit

    /**
     * Refreshes all features from their underlying sources.
     *
     * ### Example
     * ```kotlin
     * scope.launch {
     *     while (isActive) {
     *         delay(5.minutes)
     *         observable.refresh()
     *             .onSuccess { logger.info("Features refreshed") }
     *             .onFailure { logger.error("Refresh failed", it) }
     *     }
     * }
     * ```
     *
     * @return [Result.success] if at least one source refreshed successfully
     */
    public suspend fun refresh(): Result<Unit>

    /**
     * Invalidates the cached state of a single feature.
     *
     * Forces re-evaluation from sources on next access. If the value changed,
     * observers and flows will be notified.
     *
     * @param feature Feature key to invalidate
     */
    public fun invalidate(feature: FeatureKey)

    /**
     * Invalidates all cached feature states.
     *
     * More efficient than calling [invalidate] for each feature individually.
     * Changed features will trigger notifications.
     */
    public fun invalidateAll()

    /**
     * Checks if the observable is active and ready for use.
     *
     * @return `true` if active, `false` if closed
     */
    public fun isActive(): Boolean

    public companion object {
        /**
         * Creates an [ObservableToggle] with DSL-based configuration.
         *
         * ## Example
         * ```kotlin
         * val observable = ObservableToggle {
         *     toggle = Toggle { /* ... */ }
         *     scope = viewModelScope
         *     bufferCapacity = 256
         *     logger = customLogger
         * }
         * ```
         *
         * @param builder Configuration lambda
         * @return Configured [ObservableToggle] instance
         */
        public operator fun invoke(builder: ObservableToggleScope.() -> Unit): ObservableToggle {
            return ObservableToggleScope().apply(builder).build()
        }
    }
}

/**
 * Builder for [ObservableToggle] configuration.
 *
 * Provides a fluent API for constructing [ObservableToggle] instances with
 * sensible defaults and type-safe configuration.
 *
 * ## Example
 * ```kotlin
 * val observable = ObservableToggle {
 *     toggle = myToggle
 *     scope = viewModelScope
 *     bufferCapacity = 256
 *     logger = customLogger
 *     ownScope = false // Toggle won't manage scope lifecycle
 * }
 * ```
 */
public class ObservableToggleScope internal constructor() {
    /**
     * The underlying [Toggle] instance (required).
     */
    public var toggle: Toggle? = null

    /**
     * Coroutine scope for flow emissions.
     * If not provided, a default scope with [SupervisorJob] is created.
     */
    public var scope: CoroutineScope? = null

    /**
     * Buffer capacity for [ObservableToggle.changes] flow.
     * Defaults to 64. Increase for high-frequency updates.
     */
    public var bufferCapacity: Int = 64

    /**
     * Logger for diagnostic messages.
     * If not provided, uses platform default logger.
     */
    public var logger: ToggleLogger? = null

    /**
     * Whether [ObservableToggle] owns the scope lifecycle.
     * If true, scope will be cancelled on [ObservableToggle.close].
     * Defaults to true if no scope is provided, false otherwise.
     */
    public var ownScope: Boolean? = null

    internal fun build(): ObservableToggle {
        val toggle = requireNotNull(toggle) { "toggle must be provided" }
        require(bufferCapacity > 0) { "bufferCapacity must be positive" }

        val actualScope = scope ?: CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val shouldOwnScope = ownScope ?: (scope == null)
        val actualLogger = logger ?: createPlatformLogger()

        return DefaultObservableToggle(
            toggle = toggle,
            scope = actualScope,
            bufferCapacity = bufferCapacity,
            logger = actualLogger,
            ownScope = shouldOwnScope
        )
    }
}

/**
 * A reactive and observable layer built on top of the core [Toggle] engine.
 *
 * This implementation uses [StateFlow] and [SharedFlow] to provide reactive observation
 * for feature flag states, notifying consumers and external [FeatureObserver]s of changes.
 *
 *
 * ### Memory Efficiency
 * - Uses [ImmutableMap] for feature states (structural sharing)
 * - Per-feature StateFlow caching to avoid duplicate flows
 * - Automatic cleanup of empty observer lists
 *
 * ### Performance Characteristics
 * - O(1) feature state lookup
 * - O(n) refresh where n = number of changed features
 * - Observer notifications run on provided scope
 *
 * @property toggle The underlying core [Toggle] engine instance
 * @property scope The [CoroutineScope] for asynchronous flow emissions
 * @property bufferCapacity Buffer size for change events SharedFlow
 * @property logger Logger for diagnostic messages
 * @property ownScope Whether to cancel scope on close
 */
internal class DefaultObservableToggle(
    private val toggle: Toggle,
    private val scope: CoroutineScope,
    private val bufferCapacity: Int,
    private val logger: ToggleLogger,
    private val ownScope: Boolean
) : ObservableToggle {
    // Concurrent map for thread-safe observer management
    // Using AtomicFu locks for multiplatform compatibility
    private val observersLock = reentrantLock()
    private val observers = mutableMapOf<String, MutableList<FeatureObserver>>()

    // Backing field for features StateFlow
    private val _features = MutableStateFlow<ImmutableMap<String, Boolean>>(persistentMapOf())

    override val features: StateFlow<ImmutableMap<String, Boolean>>
        get() = _features.asStateFlow()

    // Backing field for changes SharedFlow
    private val _changes = MutableSharedFlow<FeatureChangeEvent>(
        replay = 0,
        extraBufferCapacity = bufferCapacity,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override val changes: SharedFlow<FeatureChangeEvent>
        get() = _changes.asSharedFlow()

    // Cache for per-feature StateFlows to avoid creating duplicates
    // Uses internal synchronization via observersLock
    private val featureFlowCache = mutableMapOf<String, StateFlow<Boolean>>()

    // Atomic lifecycle state
    private val isActiveAtomic = atomic(true)

    init {
        updateInternalStates(getAllFeatures())
    }

    override fun isEnabled(feature: FeatureKey): Boolean {
        if (!isActive()) return false
        return runCatching {
            toggle.isEnabled(feature)
        }.getOrElse { error ->
            logger.log("Failed to evaluate feature '${feature.value}': ${error.message}", error)
            false
        }
    }

    override fun isEnabled(key: String): Boolean {
        if (!isActive()) return false
        return runCatching {
            toggle.isEnabled(key)
        }.getOrElse { error ->
            logger.log("Failed to evaluate feature '$key': ${error.message}", error)
            false
        }
    }

    override fun observe(feature: FeatureKey): StateFlow<Boolean> {
        val key = feature.value

        // Return cached flow if available
        val cached = observersLock.withLock { featureFlowCache[key] }
        if (cached != null) return cached

        // Create new flow
        val featureFlow = features
            .map { states -> states[key] == true }
            .distinctUntilChanged()
            .stateIn(
                scope = scope,
                started = SharingStarted.Lazily,
                initialValue = isEnabled(feature)
            )

        // Cache it with double-check pattern
        return observersLock.withLock {
            featureFlowCache.getOrPut(key) { featureFlow }
        }
    }

    override fun addObserver(featureKey: String, observer: FeatureObserver): () -> Unit {
        observersLock.withLock {
            observers.getOrPut(featureKey) { mutableListOf() }.add(observer)
        }

        // Return unsubscribe function
        return {
            observersLock.withLock {
                observers[featureKey]?.remove(observer)
                // Clean up empty list to prevent map bloat
                if (observers[featureKey]?.isEmpty() == true) {
                    observers.remove(featureKey)
                }
            }
        }
    }

    override suspend fun refresh(): Result<Unit> {
        check(isActiveAtomic.value) { "Cannot refresh: Observable is closed." }
        val oldStates = _features.value

        return toggle.refresh().onSuccess {
            val newStates = getAllFeatures()
            updateInternalStates(newStates)
            notifyChanges(oldStates, newStates)
        }
    }

    override fun invalidate(feature: FeatureKey) {
        check(isActiveAtomic.value) { "Cannot invalidate: Observable is closed." }
        val oldValue = isEnabled(feature)
        toggle.invalidate(feature)
        val newValue = isEnabled(feature)

        if (oldValue != newValue) {
            updateInternalStates(getAllFeatures())
            notifyObservers(feature.value, oldValue, newValue)
        }
    }

    override fun invalidateAll() {
        check(isActiveAtomic.value) { "Cannot invalidate: Observable is closed." }
        val oldStates = _features.value
        toggle.invalidateAll()
        val newStates = getAllFeatures()
        updateInternalStates(newStates)
        notifyChanges(oldStates, newStates)
    }

    /**
     * Gets all feature states from the underlying core toggle.
     */
    private fun getAllFeatures(): ImmutableMap<String, Boolean> {
        return toggle.values().mapValues { it.value.enabled }.toImmutableMap()
    }

    /**
     * Updates the reactive feature states [features] with the new map.
     */
    private fun updateInternalStates(newStates: ImmutableMap<String, Boolean>) {
        _features.value = newStates
    }

    /**
     * Compares old and new states and triggers notifications for all changes.
     */
    private fun notifyChanges(oldStates: ImmutableMap<String, Boolean>, newStates: ImmutableMap<String, Boolean>) {
        val allKeys = oldStates.keys + newStates.keys
        for (key in allKeys) {
            val oldValue = oldStates[key] == true
            val newValue = newStates[key] == true
            if (oldValue != newValue) {
                notifyObservers(key, oldValue, newValue)
            }
        }
    }

    /**
     * Notifies all reactive streams and explicit observers of a single feature change.
     */
    @OptIn(ExperimentalTime::class)
    private fun notifyObservers(key: String, oldValue: Boolean, newValue: Boolean) {
        val event = FeatureChangeEvent(key, oldValue, newValue, Clock.System.now().epochSeconds)

        // 1. Emit to reactive SharedFlow stream on the provided scope.
        scope.launch {
            _changes.emit(event)
        }

        // 2. Notify registered explicit observers (must be synchronized and protected).
        observersLock.withLock {
            observers[key]?.forEach { observer ->
                runCatching {
                    observer.onChanged(key, oldValue, newValue)
                }.onFailure { error ->
                    // A Senior Engineer ensures exceptions in user-provided code
                    // do not crash the core logic.
                    logger.log("FeatureObserver failed for key '$key': ${error.message}", error)
                }
            }
        }
    }

    override fun isActive(): Boolean = isActiveAtomic.value

    override fun close() {
        if (!isActiveAtomic.compareAndSet(expect = true, update = false)) {
            return // Already closed
        }

        observersLock.withLock {
            observers.clear()
            featureFlowCache.clear()
        }

        if (ownScope) {
            scope.cancel()
        }

        toggle.close()
        logger.log("ObservableToggle closed")
    }
}
