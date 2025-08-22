package me.shadykhalifa.whispertop.utils

import java.util.UUID

actual object UuidGenerator {
    actual fun randomUUID(): String = UUID.randomUUID().toString()
}