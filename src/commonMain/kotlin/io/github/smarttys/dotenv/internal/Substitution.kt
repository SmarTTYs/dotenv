package io.github.smarttys.dotenv.internal

import io.github.smarttys.dotenv.EnvMap
import io.github.smarttys.dotenv.exception.InvalidSubstitutionException

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

internal fun expandVariables(key: String, value: String, ignoreMalformed: Boolean, envMap: EnvMap): String {
    return value.replace(expandVariablesRegex) { matchResult ->
        matchResult.substitution(key, ignoreMalformed, envMap)
    }
}

private fun MatchResult.substitution(key: String, ignoreMalformed: Boolean, map: EnvMap): String {
    if (this.groupValues[1] == "\\") {
        return this.value.substring(1)
    }
    /**
     * Get the expanded environment variable key or return the original value
     */
    val expandedKey = this.groups[4]?.value ?: return this.value
    val currentValue = map[expandedKey]

    fun currentOrDefault() = currentValue ?: ""

    val (operator, replace) = if (this.groups[3] != null) { // opening curly braces
        if (this.groups[7] == null) { // closing curly braces
            if (!ignoreMalformed) throwMissingClosingBraces(key)
            return this.value
        }

        val operator = this.groups[5]?.value ?: return currentOrDefault()
        val alternative = this.groupValues[6]

        Pair(operator, alternative)
    } else return currentOrDefault()

    fun nestedTransform(): String {
        return if (replace.startsWith('$')) {
            replace.replace(expandVariablesRegex) {
                it.substitution(key, ignoreMalformed, map)
            }
        } else replace
    }

    if (replace.startsWith("$")) {
        val nested = replace.replace(expandVariablesRegex) {
            it.substitution(key, ignoreMalformed, map)
        }
        println("ResolvedNested: $nested -> ${nestedTransform()}")
    }

    return when (operator) {
        DEFAULT_UNSET_OPERATOR -> currentValue ?: replace
        DEFAULT_UNSET_OR_EMPTY_OPERATOR -> currentValue.takeUnless(String?::isNullOrBlank) ?: replace

        REQUIRE_VALUE_OPERATOR -> currentValue ?: throwMissingVariable(expandedKey, replace)
        REQUIRE_NON_EMPTY_OPERATOR -> {
            currentValue.takeUnless(String?::isNullOrBlank) ?: throwMissingVariable(expandedKey, replace)
        }
        SUBSTRING_OPERATOR -> {
            val value = currentValue ?: return ""

            val lengthSegment = replace.split(':')
            val startIndex = lengthSegment.first().toIntOrThrow(key, 0, ignoreMalformed)
            val endIndex = lengthSegment.getOrNull(1).toIntOrThrow(key, value.length, ignoreMalformed)

            value.substring(startIndex, endIndex)
        }

        else -> {
            if (!ignoreMalformed) throwUnknownSubstitutionOperator(key, operator)
            currentOrDefault()
        }
    }
}

private fun throwMissingClosingBraces(key: String): Nothing =
    throw InvalidSubstitutionException("Expected closing curly braces for value from key $key!")

private fun throwUnknownSubstitutionOperator(key: String, operator: String): Nothing =
    throw InvalidSubstitutionException("Unknown operator '$operator' for parameter substitution at key '$key'!")

private fun throwMissingVariable(key: String, error: String): Nothing =
    throw InvalidSubstitutionException("Expected element ($key): $error")

private fun throwInvalidSubstringSubstitution(key: String, value: String): Nothing =
    throw InvalidSubstitutionException("Expected number for substring substitution of key $key but got '$value'!")

private fun String?.toIntOrThrow(key: String, default: Int, ignoreMalformedSubstitution: Boolean): Int {
    val input = this ?: return default
    return input.toIntOrNull() ?: if (ignoreMalformedSubstitution) {
        default
    } else throwInvalidSubstringSubstitution(key, input)
}
