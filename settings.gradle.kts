import kotlin.text.set
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        mavenLocal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenLocal()
        mavenCentral()
    }
}

rootProject.name = "toggle"
include(":toggle-core")
include(":samples:android-sample")
include(":samples:kmp-sample")
include(":toggle-compose")
