package io.github.smarttys.dotenv.internal

internal actual fun writeEnvToFile(filePath: String, input: String) {
    println("write file")
    fs.open(filePath, "r") { err, f ->
        println("Done!")
    }

    val writeStream = fs.createWriteStream("JournalDEV.txt")
    writeStream.write("Hi, JournalDEV Users.")
    writeStream.write("Thank You.")
    writeStream.end()

    fs.writeFile(filePath, input) { err ->
        if (err as Boolean) throw err as Throwable
    }
}
