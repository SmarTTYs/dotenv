package io.github.smarttys.dotenv.internal

import io.github.smarttys.dotenv.EnvMap

internal actual fun loadEnvironmentToSystem(envMap: EnvMap, overwrite: Boolean) {
    for ((key, value) in envMap) {
        if (System.getProperty(key) != null) {
            if (overwrite) {
                System.setProperty(key, value)
            }
        } else {
            System.setProperty(key, value)
        }
    }
}
