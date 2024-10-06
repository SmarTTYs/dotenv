package io.github.smarttys.dotenv.internal

import platform.posix._putenv_s

public actual fun nativeSetEnv(key: String, value: String) {
    _putenv_s(_Name = key, _Value = value)
}
