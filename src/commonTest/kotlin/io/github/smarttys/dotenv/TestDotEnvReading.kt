package io.github.smarttys.dotenv

import io.github.smarttys.dotenv.exception.DotEnvException
import io.github.smarttys.dotenv.exception.MissingEnvFileException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TestDotEnvReading {

    @Test
    fun testMissingValue() {
        val dotEnv = DotEnv {
            testDirectory = "./assets/"

            file("plain.env")
        }

        assertNull(dotEnv["missingKey"])
        assertEquals("missing", dotEnv.getOrElse("missingKey") { "missing" })

        val getValueResult = runCatching {
            dotEnv.getOrThrow("missing")
        }

        assertTrue {
            val exception = getValueResult.exceptionOrNull() ?: return@assertTrue false
            NoSuchElementException::class.isInstance(exception)
        }
    }

    @Test
    fun testValueTransformation() {
        val dotEnv = DotEnv {
            testDirectory = "./assets/"

            file("plain.env")
        }

        val value = dotEnv["TEST", { it.lowercase() }]
        assertEquals("test", value)
    }

    @Test
    fun testSystemOverloading() {
        loadEnvironmentToSystem(mapOf("KEY" to "EXISTING", "KEY_2" to "SECOND_EXISTING"))

        val nonOverwritingDotEnv = DotEnv {
            testDirectory = "./assets/"
            overloadSystemEnvironment = true
            overwriteExistingSystemVariables = false

            file("plain.env")
        }

        assertEquals("VALUE", nonOverwritingDotEnv["KEY"])
        assertEquals("EXISTING", readFromSystem("KEY"))

        /**
         * Loads properties into environment
         */
        LoadEnv {
            testDirectory = "./assets/"
            file("plain.env")
        }

        assertEquals(nonOverwritingDotEnv["KEY"], readFromSystem("KEY"))
    }

    @Test
    fun testSystemEnvironmentInclusion() {
        loadEnvironmentToSystem(mapOf("SYSTEM_ENV" to "TEST"))

        val dotEnv = DotEnv {
            testDirectory = "./assets/substitution"
            file("substitution.env")

            includeSystemEnvironment = true
        }

        assertEquals("TEST-Test", dotEnv["FROM_ENV"])
    }

    @Test
    fun testDirectLoading() {
        /**
         * Load all key, value pairs from the file into the system
         */
        LoadEnv {
            testDirectory = "./assets/"
            file("plain.env")
        }

        assertEquals("VALUE", readFromSystem("KEY"))
    }

    @Test
    fun testLoadingIntoSystemEnvironmentAfterwards() {
        val dotEnv = DotEnv {
            testDirectory = "./assets/"
            file("plain.env")
        }

        /**
         * Load values from [DotEnv] instance into the system
         * environment.
         */
        dotEnv.loadIntoSystemEnvironment()

        assertEquals("VALUE", readFromSystem("KEY"))
    }

    @Test
    fun testDelegationNamingConvention() {
        val dotEnv = DotEnv {
            testDirectory = "./assets"
            file("plain.env")
        }

        val test by dotEnv
        assertEquals("TEST", test)
    }

    /**
     * Test variable expansion for file names.
     */
    @Test
    fun testFileNameExpansion() {
        /**
         * We write the ASSET_DIR variable into the current process
         * environment variable in order to user it later one.
         */
        loadEnvironmentToSystem(
            mapOf(
                "ASSET_DIR" to extractTestFilePath("./assets"),
                "FILE_NAME" to "plain.env"
            )
        )

        val dotEnv = DotEnv {
            /**
             * In this case ASSET_DIR will get expanded to "assets"
             */
            directory = "\${ASSET_DIR}"
            file("\${FILE_NAME}")
        }

        assertEquals("VALUE", dotEnv["KEY"])
    }

    @Test
    fun testCommentParsing() {
        val expectedValues = buildMap {
            put("NO_COMMENT_IN_UNQUOTED", "foo")
            put("NO_COMMENT_IN_UNQUOTED_WITH_TRAILING_SPACE", "foo")

            put("INLINE_COMMENT_IN_UNQUOTED", "foo")
            put("INLINE_COMMENT_IN_SPACED_UNQUOTED", "foo bar")

            put("INVALID_INLINE_COMMENT_IN_UNQUOTED", "foo#invalid inline comment")

            put("SINGLE_QUOTED_INLINE_COMMENT", "foo")
            put("DOUBLE_QUOTED_INLINE_COMMENT", "foo")

            put("SINGLE_QUOTED_WRAPPED_COMMENT", "FOO #bar")
            put("DOUBLE_QUOTED_WRAPPED_COMMENT", "FOO #bar")
        }

        loadAndCompareValues("./assets/", "comments.env", expectedValues)
    }

    @Test
    fun testQuotationParsing() {
        val expectedValues = buildMap {
            val pairs = listOf(
                "SINGLE_ESCAPING" to """A \\a test ending ' \$ test \c end\n""",

                "DOUBLE_ESCAPING" to "B \\a test $ test c end\n",
                "UNQUOTED_ESCAPING" to """C \\a test $ test \c end\n""",

                "UNESCAPED_SINGLE_QUOTES_VALUE" to """Let's go!""",
                "UNESCAPED_DOUBLE_QUOTES_JSON" to """{"hello": "A"}""",
                "ESCAPED_DOUBLE_QUOTES_JSON" to """{\"hello\": \"B\"}""",

                "VAR_1" to "some\tvaluea",
                "VAR_2" to """some\tvalue\a""",
                "VAR_3" to """some\tvalue\a"""
            )

            putAll(pairs)
        }

        loadAndCompareValues("./assets/", "quoted.env", expectedValues)
    }

    @Test
    fun testIgnoreBlankOption() {
        val dotEnv = DotEnv {
            testDirectory = "./assets/options"
            file("ignore_empty_option_test.env")

            decodeBlankValues = false
        }

        assertFalse(dotEnv.contains("UNQUOTED_EMPTY_VALUE"))
        assertFalse(dotEnv.contains("SINGLE_QUOTED_EMPTY_VALUE"))
        assertFalse(dotEnv.contains("DOUBLE_QUOTED_EMPTY_VALUE"))
        assertEquals("FOO", dotEnv["NON_EMPTY_VALUE"])
    }

    @Test
    fun testDuplicateKeys() {
        val dotEnv = runCatching {
            DotEnv {
                file("assets/duplicate-keys.test.env")

                ignoreDuplicateKeys = false
            }
        }

        assertTrue {
            val exception = dotEnv.exceptionOrNull() ?: return@assertTrue false
            DotEnvException::class.isInstance(exception)
        }
    }

    @Test
    fun testMalformedKeys() {
        @Suppress("TestFunctionName")
        fun MalformedDotEnv(builder: DotEnvBuilder.() -> Unit) = DotEnv {
            files("malformed_key_options_test.env")
            testDirectory = "./assets/options"

            apply(builder)
        }

        /**
         * With ignoreMalformedKeys disabled (default) the creation will fail caused by
         * the malformed key
         */
        val defaultDotEnvResult = runCatching {
            MalformedDotEnv {}
        }
        assertTrue(defaultDotEnvResult.isFailure)
        assertNotNull(defaultDotEnvResult.exceptionOrNull())

        val ignoreMalformedKeysDotEnv = MalformedDotEnv {
            ignoreMalformedKeys = true
        }

        /**
         * With lenient key parsing disabled and the ignoreMalformed key flag enabled
         * results should not throw an exception and be not present in the created env
         * instance.
         */
        assertNull(ignoreMalformedKeysDotEnv["MALFORMED-KEY"])

        val lenientDotEnv = MalformedDotEnv {
            lenientKeyParsing = true
        }

        /**
         * With lenient key parsing enabled also malformed keys will get added to the
         * created [DotEnv] instance.
         */
        assertNotNull(lenientDotEnv["MALFORMED-KEY"])
    }

    @Test
    fun testReadingMultipleFiles() {
        val dotEnv = DotEnv {
            files("plain.env", "comments.env")

            testDirectory = "./assets"
            ignoreMissingFile = false
        }

        // from plain.env
        assertEquals("VALUE", dotEnv["KEY"])

        // from comments.env
        assertEquals("foo", dotEnv["NO_COMMENT_IN_UNQUOTED"])
    }

    @Test
    fun testValueOverwriting() {
        val expectedValues = buildMap {
            put("HOST", "staging.test.io")
            put("PORT", "3306")
        }

        val testEnvFile = DotEnv {
            testDirectory = "./assets"
            file("overwriting/test.env")
        }
        val stagingEnvFile = DotEnv {
            testDirectory = "./assets"
            file("overwriting/staging.env")
        }
        val combinedEnv = testEnvFile + stagingEnvFile

        /**
         * Host value from test 'test.env' file gets overwritten by the value in the 'staging.env' file
         */
        loadAndCompareValues(combinedEnv, expectedValues)
    }

    @Test
    fun testMissingFileException() {
        val result = runCatching {
            DotEnv {
                testDirectory = "./assets"
                file("missing_file.env")
                ignoreMissingFile = false
            }
        }

        assertTrue {
            val exception = result.exceptionOrNull() ?: return@assertTrue false
            MissingEnvFileException::class.isInstance(exception)
        }
    }

    @Test
    fun testMultilineValue() {
        val expectedValues = buildMap {
            put(
                "PRIVATE_KEY",
                """
                    -----BEGIN RSA PRIVATE KEY-----
                    ...
                    xHN5c...
                    ...
                    -----END RSA PRIVATE KEY-----
                """.trimIndent()
            )
        }

        val dotEnv = DotEnv {
            testDirectory = "./assets"
            file("plain.env")
        }

        val replaceSeparatorValue = dotEnv["PRIVATE_KEY"]?.replace("\r\n", "\n")
        assertEquals(expectedValues["PRIVATE_KEY"], replaceSeparatorValue)
    }

    @Test
    fun testCustomValue() {
        val exampleValue = "Value=Test"
        loadEnvironmentToSystem(mapOf("TEST" to exampleValue))

        var mapValue = readFromSystem("TEST")
        assertEquals(exampleValue, mapValue)

        /**
         * Currently defined value should not get overwritten
         */
        io.github.smarttys.dotenv.internal.loadEnvironmentToSystem(mapOf("TEST" to "Another"), overwrite = false)

        mapValue = readFromSystem("TEST")
        assertEquals(exampleValue, mapValue)

        /**
         * Currently defined value should get overwritten
         */
        io.github.smarttys.dotenv.internal.loadEnvironmentToSystem(mapOf("TEST" to "Another"), overwrite = true)

        mapValue = readFromSystem("TEST")
        assertEquals("Another", mapValue)
    }

    private fun loadAndCompareValues(path: String, fileName: String, expectedValues: EnvMap) {
        val dotEnv = DotEnv {
            testDirectory = path
            file(fileName)
        }
        loadAndCompareValues(dotEnv, expectedValues)
    }

    private fun loadAndCompareValues(dotEnv: DotEnv, expectedValues: EnvMap) {
        for ((key, value) in expectedValues) {
            assertEquals(value, dotEnv[key], "Validation for key $key failed")
        }
    }
}
