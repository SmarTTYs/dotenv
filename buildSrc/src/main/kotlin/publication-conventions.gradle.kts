plugins {
    signing
    `maven-publish`
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier = "javadoc"
}

publishing {
    repositories {
        maven("https://maven.pkg.github.com/SmarTTYs/dotenv") {
            name = "GitHubPackages"
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("TOKEN")
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
            pom.configureMavenMetaData(rootProject)
        }

        /**
         * Apply signing to all publications
         */
        publications.withType<MavenPublication>().forEach(::signPublicationIfKeyPresent)
    }
}

fun MavenPom.configureMavenMetaData(project: Project) {
    name = project.name
    description = "Kotlin multiplatform library to load environment variables from .env files"
    url = "https://github.com/SmarTTYs/dotenv"

    licenses {
        license {
            name = "The Apache Software License, Version 2.0"
            url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
            distribution = "repo"
        }
    }

    issueManagement {
        system = "Github"
        url = "https://github.com/SmarTTYs/dotenv/issues"
    }

    developers {
        developer {
            id = "SmarTTYs"
            name = "SmarTTYs"
            url = "https://github.com/SmarTTYs/"
        }
    }

    scm {
        url = "https://github.com/SmarTTYs/dotenv"
        connection = "scm:git:git://github.com/SmarTTYs/dotenv.git"
        developerConnection = "scm:git:git@github.com:SmarTTYs/dotenv.git"
    }
}

fun signPublicationIfKeyPresent(publication: MavenPublication) {
    val keyId = rootProject.getSensitiveProperty("libs.sign.key.id")
    val signingKey = rootProject.getSensitiveProperty("libs.sign.key.private")
    val signingKeyPassphrase = rootProject.getSensitiveProperty("libs.sign.passphrase")
    if (!signingKey.isNullOrBlank()) {
        rootProject.extensions.configure<SigningExtension>("signing") {
            useInMemoryPgpKeys(keyId, signingKey, signingKeyPassphrase)
            sign(publication)
        }
    }
}

private fun Project.getSensitiveProperty(name: String): String? {
    return project.findProperty(name) as? String ?: System.getenv(name)
}
