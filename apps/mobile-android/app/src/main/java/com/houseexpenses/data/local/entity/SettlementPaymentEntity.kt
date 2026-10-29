package com.houseexpenses.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settlement_payments")
data class SettlementPaymentEntity(
    @PrimaryKey
    val id: String,
    val householdId: String,
    val payerUserId: String,
    val receiverUserId: String,
    val payerName: String = "",
    val receiverName: String = "",
    val amount: Double,
    val channel: String = "Transferencia",
    val date: String,
    val notes: String = "",
    val syncStatus: String = "PENDING", // "PENDING", "PENDING_SYNC", "SYNCED"
    val createdAt: Long = System.currentTimeMillis()
)
