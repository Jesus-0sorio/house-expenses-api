package com.houseexpenses.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "incomes")
data class IncomeEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val amount: Double,
    val type: String, // "SALARY_REGULAR", "PRIMA", "BONUS", "FREELANCE"
    val date: String,
    val description: String,
    val syncStatus: String = "PENDING", // "PENDING", "SYNCED"
    val createdAt: Long = System.currentTimeMillis()
)
