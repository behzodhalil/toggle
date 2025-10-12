plugins {
    //trick: for the same plugin versions in all sub-modules
    alias(libs.plugins.androidLibrary).apply(false)
    alias(libs.plugins.kotlinMultiplatform).apply(false)
    kotlin("plugin.serialization") version "2.2.0"
    alias(libs.plugins.compose.multiplatform).apply(false)
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.vanniktech.publish) apply false
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.18.1"
}

allprojects {
    group = "io.github.behzodhalil"
    version = "0.1.0"

    repositories {
        google()

        mavenCentral()
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}