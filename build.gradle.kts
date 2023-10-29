import org.jetbrains.dokka.gradle.DokkaTask
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

tasks.wrapper {
    gradleVersion = "8.4"
}

kotlin {
    jvmToolchain(11)
    jvm {
        /*
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
        withJava()
        */

        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }
    js(IR) {
        nodejs {
            testTask(
                Action {
                    environment("TEST", "TEST")
                }
            )
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
            title = "DotEnv HTML Report"
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

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}

publishing {
    repositories {
        // System.getenv("libs.repository.id")
        maven("https://maven.pkg.github.com/SmarTTYs/dotenv") {
            name = "GitHubPackages"
            credentials {
                username = getSensitiveProperty("GITHUB_ACTOR")
                password = getSensitiveProperty("GITHUB_TOKEN")
            }
        }
    }

    publications {
        val version: String by rootProject
        val group: String by rootProject

        create<MavenPublication>("maven") {
            this.version = version
            this.groupId = group
            this.artifactId = project.name

            artifact(javadocJar.get())

            /**
             * Configure maven pom
             */
            pom {
                configureMavenMetaData(project)
            }

            /**
             * Apply signing
             */
            signPublicationIfKeyPresent(project, this)
        }
    }
}

fun MavenPom.configureMavenMetaData(project: Project) {
    name by project.name
    description by "Kotlin multiplatform library to load environment variables from .env files"
    url by "https://github.com/SmarTTYs/dotenv"

    licenses {
        license {
            name by "The Apache Software License, Version 2.0"
            url by "https://www.apache.org/licenses/LICENSE-2.0.txt"
            distribution by "repo"
        }
    }

    issueManagement {
        system.set("Github")
        url.set("https://github.com/SmarTTYs/dotenv/issues")
    }

    developers {
        developer {
            id by "SmarTTYs"
            name by "SmarTTYs"
            url by "https://github.com/SmarTTYs/"
        }
    }

    scm {
        url by "https://github.com/SmarTTYs/dotenv"
        connection by "scm:git:git://github.com/SmarTTYs/dotenv.git"
        developerConnection by "scm:git:git@github.com:SmarTTYs/dotenv.git"
    }
}

fun signPublicationIfKeyPresent(project: Project, publication: MavenPublication) {
    val keyId = project.getSensitiveProperty("libs.sign.key.id")
    val signingKey = project.getSensitiveProperty("libs.sign.key.private")
    val signingKeyPassphrase = project.getSensitiveProperty("libs.sign.passphrase")
    if (!signingKey.isNullOrBlank()) {
        project.extensions.configure<SigningExtension>("signing") {
            useInMemoryPgpKeys(keyId, signingKey, signingKeyPassphrase)
            sign(publication)
        }
    }
}

infix fun <T> Property<T>.by(value: T) {
    set(value)
}

fun Project.getSensitiveProperty(name: String): String? {
    return project.findProperty(name) as? String ?: System.getenv(name)
}
