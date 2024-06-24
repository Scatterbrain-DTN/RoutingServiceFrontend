package net.ballmerlabs.scatterroutingservice.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import java.util.UUID

@Dao
interface LocalChatsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(localChat: LocalChat)

    @Query("SELECT * FROM local_chats WHERE uuid IN (:uuid)")
    suspend fun getByUuid(uuid: List<UUID>): List<LocalChat>

    suspend fun getSortedByUuid(uuid: List<UUID>): Map<UUID, LocalChat> {
        return getByUuid(uuid).associateBy { v -> v.uuid }
    }

}