package com.houseexpenses.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val householdId: String? = null,
    val invoiceId: String? = null,
    val amount: Double,
    val date: String,
    val category: String,
    val description: String,
    val scope: String, // "PERSONAL" or "HOUSEHOLD"
    val paymentStatus: String = "PAID",
    val syncStatus: String = "PENDING", // "PENDING", "SYNCED"
    val createdAt: Long = System.currentTimeMillis()
)
