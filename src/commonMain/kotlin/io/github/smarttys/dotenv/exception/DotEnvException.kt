package io.github.smarttys.dotenv.exception

public class DotEnvException internal constructor(override var message: String) : IllegalArgumentException()

internal typealias KeyFormatErrorCode = Int
internal const val MISSING_KEY_SEPARATOR_SIGN: KeyFormatErrorCode = 2
internal const val INVALID_KEY_FORMAT: KeyFormatErrorCode = 1
