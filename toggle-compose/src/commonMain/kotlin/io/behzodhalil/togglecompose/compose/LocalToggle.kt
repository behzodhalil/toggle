package io.behzodhalil.togglecompose.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import io.behzodhalil.togglecore.core.FeatureKey
import io.behzodhalil.togglecore.core.Toggle
import io.behzodhalil.togglecore.observer.ObservableToggle

/**
 * CompositionLocal for providing [Toggle] down the composition tree.
 *
 * Use [LocalToggle.current] to access the current Toggle instance from any composable.
 *
 * ## Example: Providing Toggle
 * ```kotlin
 * @Composable
 * fun App() {
 *     val toggle = rememberToggle { /* config */ }
 *
 *     CompositionLocalProvider(LocalToggle provides toggle) {
 *         // All child composables can access toggle via LocalToggle.current
 *         HomeScreen()
 *     }
 * }
 * ```
 *
 * ## Example: Accessing Toggle
 * ```kotlin
 * @Composable
 * fun FeatureButton() {
 *     val toggle = LocalToggle.current
 *
 *     if (toggle.isEnabled(Features.NEW_BUTTON_STYLE)) {
 *         NewStyleButton()
 *     } else {
 *         OldStyleButton()
 *     }
 * }
 * ```
 *
 * @see LocalObservableToggle for reactive observation
 */
val LocalToggle: ProvidableCompositionLocal<Toggle?> = staticCompositionLocalOf { null }

/**
 * CompositionLocal for providing [ObservableToggle] down the composition tree.
 *
 * This is the recommended way to provide feature toggles in Compose applications
 * that need reactive updates.
 *
 * ## Example: App-Level Provider
 * ```kotlin
 * @Composable
 * fun App() {
 *     val toggle = rememberToggle { /* config */ }
 *     val observable = rememberObservableToggle(toggle)
 *
 *     CompositionLocalProvider(LocalObservableToggle provides observable) {
 *         NavigationHost()
 *     }
 * }
 * ```
 *
 * ## Example: Observing Features
 * ```kotlin
 * @Composable
 * fun ProfileScreen() {
 *     val observable = LocalObservableToggle.current
 *         ?: error("ObservableToggle not provided")
 *
 *     val isPremium by observable.observeAsState(Features.PREMIUM)
 *
 *     if (isPremium) {
 *         PremiumContent()
 *     } else {
 *         FreeContent()
 *     }
 * }
 * ```
 *
 * @see LocalToggle for non-reactive access
 */
val LocalObservableToggle: ProvidableCompositionLocal<ObservableToggle?> = compositionLocalOf { null }


/**
 * Extension function to observe a feature using [LocalObservableToggle].
 *
 * This provides a convenient way to observe features without explicitly
 * accessing LocalObservableToggle.current.
 *
 * ## Example
 * ```kotlin
 * @Composable
 * fun MyScreen() {
 *     val isDarkMode by observeToggle(Features.DARK_MODE)
 *
 *     MaterialTheme(
 *         colorScheme = if (isDarkMode) darkColorScheme() else lightColorScheme()
 *     ) {
 *         Content()
 *     }
 * }
 * ```
 *
 * @param feature The feature key to observe
 * @return State emitting the current feature value
 * @throws IllegalStateException if ObservableToggle is not provided
 */
@Composable
fun observeToggle(feature: FeatureKey): State<Boolean> {
    val observable = LocalObservableToggle.current
        ?: error("ObservableToggle not provided. Wrap your composable with ToggleProvider.")

    return observable.observeAsState(feature)
}

/**
 * Extension function to check if a feature is enabled using [LocalToggle].
 *
 * This is a non-reactive check. Use [observeToggle] for reactive updates.
 *
 * ## Example
 * ```kotlin
 * @Composable
 * fun MyButton() {
 *     if (isFeatureEnabled(Features.NEW_BUTTON_STYLE)) {
 *         NewButton()
 *     } else {
 *         OldButton()
 *     }
 * }
 * ```
 *
 * @param feature The feature key to check
 * @return Current enabled state
 * @throws IllegalStateException if Toggle is not provided
 */
@Composable
fun isFeatureEnabled(feature: FeatureKey): Boolean {
    val toggle = LocalToggle.current
        ?: error("Toggle not provided. Wrap your composable with ToggleProvider.")

    return toggle.isEnabled(feature)
}