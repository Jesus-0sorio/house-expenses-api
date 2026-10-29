package com.houseexpenses

import android.app.Application
import androidx.work.*
import com.houseexpenses.worker.SyncWorker
import java.util.concurrent.TimeUnit

class HouseExpensesApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        setupPeriodicSyncWorker()
    }

    private fun setupPeriodicSyncWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicSyncWork = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "PeriodicExpenseSync",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicSyncWork
        )
    }
}
