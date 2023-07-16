package io.github.smarttys.dotenv.internal

import io.github.smarttys.dotenv.EnvMap

internal actual fun loadEnvironmentToSystem(envMap: EnvMap, overwrite: Boolean) {
    for ((key, value) in envMap) {
        if (process.env.contains(key)) {
            if (overwrite) {
                process.env[key] = value
            }
        } else {
            process.env[key] = value
        }
    }
}
