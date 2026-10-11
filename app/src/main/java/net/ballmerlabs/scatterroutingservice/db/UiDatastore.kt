package net.ballmerlabs.scatterroutingservice.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.util.UUID


class UuidTypeConverter {
    @TypeConverter
    fun uuidToString(uuid: UUID): String {
        return uuid.toString()
    }

    @TypeConverter
    fun stringToUUID(string: String): UUID {
        return UUID.fromString(string)
    }
}

/**
 * declaration of room database
 */
@Database(
    entities = [
        LocalChat::class
    ],
    version = 2,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(
            from = 1,
            to = 2
        )
    ]
)
@TypeConverters(UuidTypeConverter::class)
abstract class UiDatastore : RoomDatabase() {
    abstract fun localChatDao(): LocalChatsDao
}