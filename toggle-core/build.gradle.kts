import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties
import kotlin.apply

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    kotlin("plugin.serialization")
    alias(libs.plugins.vanniktech.publish)
}

kotlin {
    explicitApi()
    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_1_8)
                }
            }
        }
        publishLibraryVariants("release")
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "toggle-core"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
            implementation("com.charleskorn.kaml:kaml:0.96.0")
            implementation("org.jetbrains.kotlinx:atomicfu:0.26.1")
            implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.7")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
        }

        androidMain.dependencies {
            implementation("androidx.compose.runtime:runtime:1.7.5")
            implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }

}

android {
    namespace = "io.behzodhalil.togglecore"
    compileSdk = 35
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        load(localPropertiesFile.inputStream())
    }
}

localProperties.forEach { key, value ->
    val keyString = key.toString()
    when {
        keyString.startsWith("signing.") -> {
            if (keyString == "signing.secretKeyRingFile") {
                val keyFile = rootProject.file(value.toString())
                extra[keyString] = keyFile.absolutePath
            } else {
                extra[keyString] = value
            }
        }
        keyString == "mavenCentralUsername" || keyString == "mavenCentralPassword" -> {
            extra[keyString] = value
            println("Loaded $keyString: ${if (keyString.contains("Password")) "***" else value}")
        }
    }
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
}