import com.github.javaparser.resolution.types.ResolvedLambdaConstraintType.bound
import org.jetbrains.dokka.gradle.DokkaTask
// import org.jetbrains.dokka.gradle.DokkaTask
import java.net.URL

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
    alias(libs.plugins.dokka)

    alias(libs.plugins.kotlin.multiplatform)

    signing
    `maven-publish`
}

group = "io.github.smarttys"
version = "0.1"

repositories {
    mavenCentral()
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
        withJava()
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }
    js(IR) {
        nodejs {
            testTask {
                environment("TEST", "TEST")
            }
        }
        // browser()
        /*
        browser {
            testTask {
                useMocha()
                /*
                useKarma {
                    useChromeHeadless()
                    useFirefox()
                }
                */
            }
            /*
            commonWebpackConfig {
            }
            */
        }

         */

        binaries.executable()
    }
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosX64("native")
        hostOs == "Linux" -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        val commonMain by getting
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jvmMain by getting
        val jvmTest by getting
        val jsMain by getting
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val nativeMain by getting
        val nativeTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

tasks.withType<DokkaTask> {
    moduleName.set(project.name)
    moduleVersion.set(project.version.toString())

    dokkaSourceSets {
        configureEach {
            displayName.set(name)
            skipEmptyPackages.set(true)
            skipDeprecated.set(false)

            sourceLink {
                localDirectory.set(projectDir.resolve("src"))
                remoteUrl.set(URL("https://github.com/SmarTTYs/dotenv"))
            }
        }
    }
}

koverReport {
    defaults {
        html {
            title = "My report title"
            onCheck = false
            setReportDir(layout.buildDirectory.dir("kover-reports/html-result"))
        }

        verify {
            onCheck = true

            rule("Minimal line coverage rate in percents") {
                isEnabled = true

                entity = kotlinx.kover.gradle.plugin.dsl.GroupingEntityType.APPLICATION

                bound {
                    minValue = 70

                    metric = kotlinx.kover.gradle.plugin.dsl.MetricType.LINE
                    aggregation = kotlinx.kover.gradle.plugin.dsl.AggregationType.COVERED_PERCENTAGE
                }
            }
        }
    }
}
