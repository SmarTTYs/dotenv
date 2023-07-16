package io.github.smarttys.dotenv.internal

import kotlin.jvm.JvmInline

@JvmInline
internal value class LineCursor private constructor(val position: Int) {
    constructor() : this(1)

    operator fun inc(): LineCursor = LineCursor(position + 1)
}
