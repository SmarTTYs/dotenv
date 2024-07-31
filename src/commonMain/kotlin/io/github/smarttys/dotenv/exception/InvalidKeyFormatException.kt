package io.github.smarttys.dotenv.exception

public class InvalidKeyFormatException internal constructor(message: String) : DotEnvException(message)

internal typealias KeyFormatErrorCode = Int

/**
 *
 */
internal const val MISSING_KEY_SEPARATOR_SIGN = 2

internal const val INVALID_KEY_FORMAT = 1
