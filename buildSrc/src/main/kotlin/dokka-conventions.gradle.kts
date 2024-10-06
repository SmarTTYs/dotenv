import org.jetbrains.dokka.gradle.DokkaTask
import java.net.URI

plugins {
    id("org.jetbrains.dokka")
}

tasks.withType<DokkaTask> {
    moduleName = project.name
    moduleVersion = project.version.toString()

    dokkaSourceSets.configureEach {
        displayName = name
        skipEmptyPackages = true
        skipDeprecated = false

        sourceLink {
            localDirectory = projectDir.resolve("src")
            remoteUrl = URI("https://github.com/SmarTTYs/dotenv").toURL()
        }
    }
}
