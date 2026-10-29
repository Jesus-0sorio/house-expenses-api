package com.houseexpenses.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val entityType: String, // "EXPENSE", "INCOME"
    val entityId: String,
    val action: String, // "CREATE", "UPDATE"
    val payloadJson: String,
    val timestamp: Long = System.currentTimeMillis()
)
