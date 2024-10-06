package io.github.smarttys.dotenv.internal

internal actual fun writeEnvToFile(filePath: String, input: String) {
    println("write file")
    fs.open(filePath, "r") { err, f ->
        println("Done!")
    }

    fs.writeFile(filePath, input) { err ->
        if (err as Boolean) throw err as Throwable
    }
}
