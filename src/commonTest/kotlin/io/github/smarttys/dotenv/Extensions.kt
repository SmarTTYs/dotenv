package io.github.smarttys.dotenv

import io.github.smarttys.dotenv.internal.loadEnvironmentToSystem

expect fun readFromSystem(key: String): String?
expect fun checkFileExistsAndRemove(filePath: String): Boolean

expect fun extractTestFilePath(originalPath: String): String

internal fun loadEnvironmentToSystem(envMap: EnvMap) = loadEnvironmentToSystem(envMap, true)

var DotEnvBuilder.testDirectory
    get() = directory
    set(value) {
        directory = extractTestFilePath(value)
    }
