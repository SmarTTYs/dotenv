import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithTests
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeSimulatorTest
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest

plugins {
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)

    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.binary.compatibility.validator)

    id("publication-conventions")
    id("kover-conventions")
    id("dokka-conventions")
}

group = "io.github.smarttys"
version = "0.1.0"

tasks.wrapper {
    gradleVersion = "8.9"
}

android {
    namespace = "io.github.smarttys.dotenv"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}

kotlin {
    explicitApiWarning()
    compilerOptions {
        allWarningsAsErrors = true
    }

    jvmToolchain(21)

    jvm {
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    androidTarget()
    androidNativeX64()
    androidNativeX86()
    androidNativeArm32()
    androidNativeArm64()

    js(KotlinJsCompilerType.IR) {
        browser()
        nodejs()
    }

    linuxX64()
    linuxArm64()
    mingwX64()

    iosX64()
    iosArm64()
    watchosArm32()
    macosArm64()
    macosX64()
    tvosX64()
    tvosArm64()

    // setup tests running in RELEASE mode
    targets.withType<KotlinNativeTarget>().configureEach {
        binaries.test(listOf(NativeBuildType.RELEASE))
    }
    targets.withType<KotlinNativeTargetWithTests<*>>().configureEach {
        testRuns.create("releaseTest") {
            setExecutionSourceFrom(binaries.getTest(NativeBuildType.RELEASE))
        }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

tasks {
    val testEnv = mapOf("HELLO" to "HELLO", "PREFIX_HELLO" to "HELLO")
    withType<Test> {
        enabled = true
        environment(testEnv)
    }

    withType<KotlinJsTest> {
        enabled = true
        environment = testEnv.toMutableMap()
    }

    withType<KotlinNativeTest> {
        enabled = true
        environment = testEnv
    }

    withType<KotlinNativeSimulatorTest> {
        enabled = false
    }
}
