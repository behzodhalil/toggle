plugins {
    // trick: for the same plugin versions in all sub-modules
    alias(libs.plugins.androidLibrary).apply(false)
    alias(libs.plugins.kotlinMultiplatform).apply(false)
    kotlin("plugin.serialization") version "2.2.0"
    alias(libs.plugins.compose.multiplatform).apply(false)
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.vanniktech.publish) apply false
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.18.1"
    id("io.gitlab.arturbosch.detekt") version "1.23.4"
    id("org.jlleitschuh.gradle.ktlint") version "12.0.3"
}

allprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    group = "io.github.behzodhalil"
    version = "0.1.0"

    repositories {
        google()

        mavenCentral()
    }
}
detekt {
    buildUponDefaultConfig = true
    config.setFrom("$projectDir/detekt.yml")

    reports {
        html.required.set(true)
        xml.required.set(false)
        txt.required.set(false)
    }
}

ktlint {
    version.set("1.0.1")
    android.set(true)
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
