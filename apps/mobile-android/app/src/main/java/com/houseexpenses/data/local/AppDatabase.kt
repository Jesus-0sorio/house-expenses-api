package com.houseexpenses.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.houseexpenses.data.local.dao.ExpenseDao
import com.houseexpenses.data.local.dao.IncomeDao
import com.houseexpenses.data.local.dao.SettlementPaymentDao
import com.houseexpenses.data.local.dao.SyncQueueDao
import com.houseexpenses.data.local.entity.ExpenseEntity
import com.houseexpenses.data.local.entity.IncomeEntity
import com.houseexpenses.data.local.entity.SettlementPaymentEntity
import com.houseexpenses.data.local.entity.SyncQueueEntity

@Database(
    entities = [ExpenseEntity::class, IncomeEntity::class, SyncQueueEntity::class, SettlementPaymentEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun incomeDao(): IncomeDao
    abstract fun settlementPaymentDao(): SettlementPaymentDao
    abstract fun syncQueueDao(): SyncQueueDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "house_expenses_local.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
