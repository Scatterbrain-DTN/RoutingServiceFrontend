package net.ballmerlabs.scatterroutingservice.db

import androidx.room.*
import net.ballmerlabs.uscatterbrain.db.entities.*
import java.util.*


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