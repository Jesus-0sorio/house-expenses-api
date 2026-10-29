package com.houseexpenses.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.houseexpenses.data.repository.ExpenseRepository

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val repository = ExpenseRepository(applicationContext)
        val success = repository.performSync()
        return if (success) {
            Result.success()
        } else {
            Result.retry()
        }
    }
}
