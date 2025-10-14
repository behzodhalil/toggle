
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
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
        iosSimulatorArm64(),
    ).forEach {
        it.binaries.framework {
            baseName = "toggle-compose"
            isStatic = true
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":toggle-core"))
                implementation(libs.kotlin.stdlib)
                implementation(compose.ui)
                implementation(compose.material)
                implementation(compose.material3)
                implementation(compose.runtime)
                implementation(compose.foundation)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        androidMain {
            dependencies {
                // Add Android-specific dependencies here. Note that this source set depends on
                // commonMain by default and will correctly pull the Android artifacts of any KMP
                // dependencies declared in commonMain.
            }
        }

        iosMain {
            dependencies {
            }
        }
    }
}

android {
    namespace = "io.behzodhalil.togglecompose"
    compileSdk = 35
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

val localProperties =
    Properties().apply {
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
                ext[keyString] = keyFile.absolutePath
            } else {
                ext[keyString] = value
            }
        }
        keyString == "mavenCentralUsername" || keyString == "mavenCentralPassword" -> {
            ext[keyString] = value
        }
    }
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
}
