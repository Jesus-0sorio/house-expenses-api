package com.houseexpenses.data.local.dao

import androidx.room.*
import com.houseexpenses.data.local.entity.SettlementPaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettlementPaymentDao {
    @Query("SELECT * FROM settlement_payments WHERE householdId = :householdId ORDER BY date DESC, createdAt DESC")
    fun getPaymentsByHousehold(householdId: String): Flow<List<SettlementPaymentEntity>>

    @Query("SELECT * FROM settlement_payments ORDER BY date DESC, createdAt DESC")
    fun getAllPayments(): Flow<List<SettlementPaymentEntity>>

    @Query("SELECT * FROM settlement_payments WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingSyncPayments(): List<SettlementPaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: SettlementPaymentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(payments: List<SettlementPaymentEntity>)

    @Query("UPDATE settlement_payments SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)

    @Query("DELETE FROM settlement_payments WHERE id = :id")
    suspend fun deletePayment(id: String)
}
