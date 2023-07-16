package io.github.smarttys.dotenv

import io.github.smarttys.dotenv.internal.readEnvironmentMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestDotEnvWriting {

    @Test
    fun testWritingDefaultEnvFile() {
        val filePath = "assets/test.native.env"
        val dotEnv = DotEnv {
            testDirectory = "./assets/"
            file("plain.env")

            ignoreDuplicateKeys = true
            ignoreMalformedKeys = true

            ignoreMalformedSubstitution = true
        }

        dotEnv.write(filePath)
        assertTrue(checkFileExistsAndRemove(filePath), "Expected outfile to exists")
    }

    @Test
    fun testLoadingToSystemEnvironment() {
        val value = "test\nwith\nmultiple\nlines"

        loadEnvironmentToSystem(mapOf("TEST" to value))
        val map = readEnvironmentMap()

        assertEquals(value, map["TEST"])
    }
}
