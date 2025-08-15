package me.shadykhalifa.whispertop

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform