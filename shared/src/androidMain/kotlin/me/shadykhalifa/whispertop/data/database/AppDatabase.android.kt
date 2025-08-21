package me.shadykhalifa.whispertop.data.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

actual fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val context = getContext()
    return Room.databaseBuilder<AppDatabase>(
        context = context,
        name = context.getDatabasePath(dbFileName).absolutePath
    )
}

private fun getContext(): Context {
    return org.koin.core.context.GlobalContext.get().get()
}