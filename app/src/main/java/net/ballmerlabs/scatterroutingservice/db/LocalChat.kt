package net.ballmerlabs.scatterroutingservice.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date
import java.util.UUID

@Entity(tableName = "local_chats")
data class LocalChat(
    @PrimaryKey
    val uuid: UUID,
    val date: Long,
    @ColumnInfo(name = "owned", defaultValue = "false")
    val owned: Boolean = false
)