plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.gradlePlugin.kover)
    // implementation(libs.gradlePlugin.dokka)
}
