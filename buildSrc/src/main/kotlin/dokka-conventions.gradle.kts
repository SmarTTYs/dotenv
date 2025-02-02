/*
import org.jetbrains.dokka.gradle.DokkaTask
import java.net.URI

plugins {
    id("org.jetbrains.dokka")
}

tasks.dokkaGeneratePublicationHtml {
    outputDirectory = layout.buildDirectory.dir("documentation/html")
}

tasks.withType<DokkaTask> {
    moduleName = project.name
    moduleVersion = project.version.toString()

    println("Sourcesets ${dokkaSourceSets.size}")

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
*/
