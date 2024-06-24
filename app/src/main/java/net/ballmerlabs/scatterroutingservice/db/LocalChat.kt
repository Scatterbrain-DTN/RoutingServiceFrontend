package net.ballmerlabs.scatterroutingservice.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "local_chats")
data class LocalChat(
    @PrimaryKey
    val uuid: UUID,
    val date: Long
)