package me.shadykhalifa.whispertop.utils

expect object StringFormatter {
    fun format(format: String, vararg args: Any): String
}