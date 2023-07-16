package io.github.smarttys.dotenv.internal

import io.github.smarttys.dotenv.EnvMap
import platform.posix._putenv_s
import platform.posix.getenv

internal actual fun loadEnvironmentToSystem(envMap: EnvMap, overwrite: Boolean) {
    for ((key, value) in envMap) {
        if (getenv(key) != null) {
            if (overwrite) {
                _putenv_s(key, value)
            }
        } else {
            _putenv_s(key, value)
        }
    }
}
