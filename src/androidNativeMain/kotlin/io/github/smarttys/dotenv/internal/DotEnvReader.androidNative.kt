package io.github.smarttys.dotenv.internal

import io.github.smarttys.dotenv.EnvMap
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import kotlinx.cinterop.toKString
import platform.posix.environ

@OptIn(ExperimentalForeignApi::class)
internal actual fun readEnvironmentMap(): EnvMap {
    val environment = environ ?: return emptyMap()

    var index = 0
    val envMap = mutableMapOf<String, String>()
    while (true) {
        val keyValuePair = environment[index++] ?: break
        val (key, value) = keyValuePair.toKString().split("=", limit = 2)
        envMap[key] = value
    }

    return envMap
}
