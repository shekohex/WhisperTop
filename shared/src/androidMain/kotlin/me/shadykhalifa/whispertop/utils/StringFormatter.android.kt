package me.shadykhalifa.whispertop.utils

import java.text.DecimalFormat

actual object StringFormatter {
    actual fun format(format: String, vararg args: Any): String {
        return String.format(format, *args)
    }
}