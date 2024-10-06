package io.github.smarttys.dotenv.internal

import io.github.smarttys.dotenv.EnvMap
import io.github.smarttys.dotenv.exception.DotEnvException

// ${VARIABLE:-default} evaluates to default if VARIABLE is unset or empty in the environment.
private const val DEFAULT_UNSET_OR_EMPTY_OPERATOR = ":-"

// ${VARIABLE-default} evaluates to default only if VARIABLE is unset in the environment.
private const val DEFAULT_UNSET_OPERATOR = "-"

// ${VARIABLE:?err} exits with an error message containing err if VARIABLE is unset or empty in the environment.
private const val REQUIRE_NON_EMPTY_OPERATOR = ":?"

// ${VARIABLE?err} exits with an error message containing err if VARIABLE is unset in the environment.
private const val REQUIRE_VALUE_OPERATOR = "?"

// ${VARIABLE:1} substring from index 1 up to the end of the variable
// ${VARIABLE:1:4} substring from index 1 upt to 4 of the variable
private const val SUBSTRING_OPERATOR = ":"

@Suppress("RegExpRedundantEscape")
private val expandVariablesRegex = Regex(
    "(\\\\)?(\\\$)(\\{)?([A-Z\\d_]+)?([:?\\-]+)?(.*(?=\\}))?(\\})?",
    RegexOption.IGNORE_CASE
)

private const val ESCAPE_GROUP = 1
private const val KEY_GROUP = 4
private const val OPENING_BRACES = 3
private const val CLOSING_BRACES = 7
private const val OPERATOR = 5
private const val ALTERNATIVE = 6

internal fun expandVariables(key: String, value: String, ignoreMalformed: Boolean, envMap: EnvMap): String {
    return value.replace(expandVariablesRegex) { matchResult ->
        matchResult.substitution(key, ignoreMalformed, envMap)
    }
}

private fun MatchResult.substitution(key: String, ignoreMalformed: Boolean, map: EnvMap): String {
    if (this.groupValues[ESCAPE_GROUP] == "\\") {
        return this.value.substring(1)
    }

    /**
     * Get the expanded environment variable key or return the original value
     */
    val expandedKey = this.groups[KEY_GROUP]?.value ?: return this.value
    val currentValue = map[expandedKey]

    fun currentOrDefault() = currentValue ?: ""

    if (this.groups[OPENING_BRACES] != null) {
        if (this.groups[CLOSING_BRACES] == null) {
            throwMissingClosingBraces(key)
        }
    } else {
        return currentOrDefault()
    }

    fun nestedTransform(value: String): String {
        return if (value.startsWith('$')) {
            expandVariables(key, value, ignoreMalformed, map)
        } else {
            value
        }
    }

    val operator = this.groups[OPERATOR]?.value ?: return currentOrDefault()
    val replace = nestedTransform(this.groupValues[ALTERNATIVE])

    return when (operator) {
        DEFAULT_UNSET_OPERATOR -> currentValue ?: replace
        DEFAULT_UNSET_OR_EMPTY_OPERATOR -> {
            currentValue.takeUnless(String?::isNullOrBlank) ?: replace
        }

        REQUIRE_VALUE_OPERATOR -> currentValue ?: throwMissingVariable(expandedKey, replace)
        REQUIRE_NON_EMPTY_OPERATOR -> {
            currentValue.takeUnless(String?::isNullOrBlank) ?: throwMissingVariable(expandedKey, replace)
        }

        SUBSTRING_OPERATOR -> {
            val value = currentValue ?: return ""

            val lengthSegment = replace.split(':', limit = 2)
            val startIndex = lengthSegment
                .first()
                .toIntOrThrow(key, 0)

            val endIndex = lengthSegment
                .getOrNull(1)
                .toIntOrThrow(key, value.length)

            value.substring(startIndex, endIndex)
        }

        else -> {
            if (!ignoreMalformed) throwUnknownSubstitutionOperator(key, operator)
            currentOrDefault()
        }
    }
}

private fun throwMissingClosingBraces(key: String): Nothing =
    throw DotEnvException("Expected closing curly braces for value from key $key!")

private fun throwUnknownSubstitutionOperator(key: String, operator: String): Nothing =
    throw DotEnvException("Unknown operator '$operator' for parameter substitution at key '$key'!")

private fun throwMissingVariable(key: String, error: String): Nothing =
    throw DotEnvException("Expected element ($key): $error")

private fun throwInvalidSubstringSubstitution(key: String, value: String): Nothing =
    throw DotEnvException("Expected number for substring substitution of key $key but got '$value'!")

private fun String?.toIntOrThrow(key: String, default: Int): Int {
    val input = this ?: return default
    return input.toIntOrNull() ?: throwInvalidSubstringSubstitution(key, input)
}
