package io.github.smarttys.dotenv.internal

import io.github.smarttys.dotenv.EnvMap

internal expect fun loadEnvironmentToSystem(envMap: EnvMap, overwrite: Boolean)
