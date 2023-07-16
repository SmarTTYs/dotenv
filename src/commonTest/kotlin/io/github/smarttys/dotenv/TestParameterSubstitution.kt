package io.github.smarttys.dotenv

import io.github.smarttys.dotenv.exception.InvalidSubstitutionException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestParameterSubstitution {

    private fun substitutionDotEnv(inputFile: String, ignoreMalformed: Boolean = true) = DotEnv {
        testDirectory = "./assets/substitution"
        file(inputFile)

        ignoreMalformedSubstitution = ignoreMalformed
    }

    private fun substitutionDotEnvCatching(inputFile: String, ignoreMalformed: Boolean = false): Result<DotEnv> {
        return runCatching { substitutionDotEnv(inputFile, ignoreMalformed) }
    }

    @Test
    fun testVariableSubstitution() {
        val dotEnv = substitutionDotEnv("test_variable_substitution.env")

        val port by dotEnv
        assertEquals("8080", port)
        assertEquals("localhost:$port", dotEnv["NORMAL"])

        assertEquals("localhost:$port", dotEnv["NORMAL_IN_PARENTHESIS"])

        /**
         * Tries to expand an undefined value leading to blank expansions result
         */
        val unsetValue = dotEnv["UNSET"]
        assertEquals("localhost:", unsetValue)

        /**
         * Tries to expand an undefined value with a specified default leading to the
         * default getting used in the expansion result
         */
        val unsetDefaultedValue = dotEnv["UNSET_WITH_DEFAULT"]
        assertEquals("localhost:42", unsetDefaultedValue)

        /**
         * Reads blank value from environment
         */
        val blankValue = dotEnv["BLANK"]
        assertEquals("", blankValue)

        /**
         * Tries to expand a blank value with a specified default leading to the
         * default getting used in the expansion result
         */
        val blankDefaultedValue = dotEnv["BLANK_VARIABLE_WITH_DEFAULT"]
        assertEquals("localhost:42", blankDefaultedValue)

        /**
         * Reads escaped substitution leading to no parameter expansion
         * getting applied
         */
        val escapedSubstitution = dotEnv["ESCAPED_SUBSTITUTION"]
        assertEquals("localhost:\${PORT}", escapedSubstitution)

        /**
         * Tries to expand defined environment variable and creates a substring
         * between from 1 until 3.
         */
        val substringOfValue = dotEnv["SUBSTRING_EXPANSION"]
        assertEquals("localhost:08", substringOfValue)

        /**
         * Tries to expand undefined value and create a substring of it.
         * This will lead to an empty string as no environment variable is defined
         */
        val substringOfUndefinedValue = dotEnv["SUBSTRING_EXPANSION_FOR_UNKNOWN"]
        assertEquals("localhost:", substringOfUndefinedValue)

        println("Test: ${dotEnv["NESTED_KEY_REPLACE"]}")
        println("Test: ${dotEnv["NESTED_KEY_REPLACE_UNKNOWN"]}")
    }

    @Test
    fun testInvalidSubstitution() {
        val result = substitutionDotEnvCatching("malformed_substitution.env")
        assertTrue {
            val exception = result.exceptionOrNull() ?: return@assertTrue false
            val validMessage = exception.message?.startsWith("Expected closing curly braces") == true
            InvalidSubstitutionException::class.isInstance(exception) && validMessage
        }

        val unknownOperatorResult = substitutionDotEnvCatching("unknown_substitution_operator.env")
        assertTrue {
            val exception = unknownOperatorResult.exceptionOrNull() ?: return@assertTrue false
            val validMessage = exception.message?.startsWith("Unknown operator") == true
            InvalidSubstitutionException::class.isInstance(exception) && validMessage
        }

        val invalidSubstringSubstitution = substitutionDotEnvCatching("invalid_substring_substitution.env")
        assertTrue {
            val exception = invalidSubstringSubstitution.exceptionOrNull() ?: return@assertTrue false
            InvalidSubstitutionException::class.isInstance(exception)
        }

        val errorSubstitution = substitutionDotEnvCatching("error_substitution.env")
        assertTrue {
            val exception = errorSubstitution.exceptionOrNull() ?: return@assertTrue false
            InvalidSubstitutionException::class.isInstance(exception)
        }
    }

    @Test
    fun testNestedSubstitution() {
        val dotEnv = substitutionDotEnv("nested_substitution_test.env")
        for ((key, value) in dotEnv) {
            println("Key: $key <> $value")
        }
    }
}
