package io.github.smarttys.dotenv.internal

import platform.posix.setenv

internal actual fun nativeSetEnv(key: String, value: String) {
    setenv(__name = key, __value = value, __replace = 1)
}
