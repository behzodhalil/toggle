plugins {
    //trick: for the same plugin versions in all sub-modules
    alias(libs.plugins.androidLibrary).apply(false)
    alias(libs.plugins.kotlinMultiplatform).apply(false)
    kotlin("plugin.serialization") version "2.2.0"
    alias(libs.plugins.compose.multiplatform).apply(false)
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
}
