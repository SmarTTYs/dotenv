package io.github.smarttys.dotenv.internal

import io.github.smarttys.dotenv.EnvMap
import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.getenv

internal expect fun nativeSetEnv(key: String, value: String)

@OptIn(ExperimentalForeignApi::class)
internal actual fun loadEnvironmentToSystem(envMap: EnvMap, overwrite: Boolean) {
    for ((key, value) in envMap) {
        if (getenv(key) != null) {
            if (overwrite) {
                nativeSetEnv(key, value)
            }
        } else {
            nativeSetEnv(key, value)
        }
    }
}
