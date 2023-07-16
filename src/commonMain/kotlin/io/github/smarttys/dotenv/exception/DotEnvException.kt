package io.github.smarttys.dotenv.exception

open class DotEnvException internal constructor(override var message: String) : IllegalArgumentException()
