package io.behzodhalil.togglecompose.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.behzodhalil.togglecore.core.FeatureKey
import io.behzodhalil.togglecore.core.Toggle
import io.behzodhalil.togglecore.core.ToggleScope
import io.behzodhalil.togglecore.observer.ObservableToggle
import io.behzodhalil.togglecore.observer.ObservableToggleScope

/**
 * Observes a feature flag and returns its current state as Compose [State].
 *
 * This is the primary API for observing feature flags in Jetpack Compose UIs.
 * The returned state will automatically trigger recomposition when the feature changes.
 *
 * ### Example
 * ```kotlin
 * @Composable
 * fun ProfileScreen(observable: ObservableToggle) {
 *     val isPremium by observable.observeAsState(Features.PREMIUM)
 *
 *     Column {
 *         ProfileHeader()
 *         if (isPremium) {
 *             PremiumBadge()
 *             PremiumFeatures()
 *         }
 *         ProfileContent()
 *     }
 * }
 * ```
 *
 * @param feature The feature key to observe
 * @return Compose [State] that emits the current boolean value of the feature
 * @see observeAsStateNonLifecycle for non-lifecycle-aware alternative
 */
@Composable
public fun ObservableToggle.observeAsState(feature: FeatureKey): State<Boolean> {
    return observe(feature).collectAsStateWithLifecycle()
}

/**
 * Observes a feature flag without lifecycle awareness.
 *
 * Use this variant when you need to observe a feature in a composable that
 * doesn't have a lifecycle (e.g., in a preview or test), or when you explicitly
 * want the observation to continue even when the UI is in the background.
 *
 * For most use cases, prefer [observeAsState] which is lifecycle-aware.
 *
 * @param feature The feature key to observe
 * @return Compose [State] that emits the current boolean value of the feature
 */
@Composable
public fun ObservableToggle.observeAsStateNonLifecycle(feature: FeatureKey): State<Boolean> {
    return observe(feature).collectAsState()
}

/**
 * Creates and remembers a Toggle instance with the given configuration.
 *
 * The Toggle will be disposed when the composable leaves the composition.
 *
 * ### Example
 * ```kotlin
 * @Composable
 * fun App() {
 *     val toggle = rememberToggle {
 *         sources {
 *             memory {
 *                 feature(Features.DARK_MODE, true)
 *             }
 *             yaml {
 *                 resourcePath = "features.yaml"
 *             }
 *         }
 *         context {
 *             userId("user_123")
 *             appVersion("2.0.0")
 *         }
 *     }
 *
 *     // Use toggle
 *     if (toggle.isEnabled(Features.DARK_MODE)) {
 *         DarkTheme()
 *     }
 * }
 * ```
 *
 * @param builder Configuration lambda for Toggle
 * @return Remembered Toggle instance
 */
@Composable
public fun rememberToggle(builder: ToggleScope.() -> Unit): Toggle {
    val toggle =
        remember {
            Toggle(builder)
        }

    DisposableEffect(toggle) {
        onDispose {
            toggle.close()
        }
    }

    return toggle
}

/**
 * Creates and remembers an ObservableToggle with the given Toggle and scope.
 *
 * The ObservableToggle will be disposed when the composable leaves the composition.
 * Uses the current composition's coroutine scope for flow emissions.
 *
 * ## Example: Basic Usage
 * ```kotlin
 * @Composable
 * fun App() {
 *     val toggle = rememberToggle { /* config */ }
 *     val observable = rememberObservableToggle(toggle)
 *
 *     val isDarkMode by observable.observeAsState(Features.DARK_MODE)
 *
 *     MaterialTheme(
 *         colorScheme = if (isDarkMode) darkColorScheme() else lightColorScheme()
 *     ) {
 *         Content()
 *     }
 * }
 * ```
 *
 * ### Example: Custom Configuration
 * ```kotlin
 * @Composable
 * fun App() {
 *     val toggle = rememberToggle { /* config */ }
 *     val observable = rememberObservableToggle(toggle) {
 *         bufferCapacity = 256  // Large buffer for high-frequency updates
 *     }
 *
 *     // Use observable
 * }
 * ```
 *
 * @param toggle The underlying Toggle instance
 * @param configure Optional configuration lambda for ObservableToggle
 * @return Remembered ObservableToggle instance
 */
@Composable
public fun rememberObservableToggle(
    toggle: Toggle,
    configure: (ObservableToggleScope.() -> Unit)? = null,
): ObservableToggle {
    val scope = rememberCoroutineScope()

    val observable =
        remember(toggle) {
            ObservableToggle {
                this.toggle = toggle
                this.scope = scope
                this.ownScope = false
                configure?.invoke(this)
            }
        }

    DisposableEffect(observable) {
        onDispose {
            observable.close()
        }
    }

    return observable
}
