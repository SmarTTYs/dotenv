package io.github.smarttys.dotenv

import io.github.smarttys.dotenv.internal.readEnvironmentMap
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestDotEnvWriting {

    @Test
    fun testWritingDefaultEnvFile() {
        fun randomName(): String {
            val base = "test.native.env"
            val suffix = (0..10).joinToString {
                Random.nextInt().toString()
            }
            return base + "_" + suffix
        }

        val filePath = "assets/${randomName()}"
        val dotEnv = DotEnv("plain.env") {
            testDirectory = "./assets/"

            ignoreDuplicateKeys = true
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
