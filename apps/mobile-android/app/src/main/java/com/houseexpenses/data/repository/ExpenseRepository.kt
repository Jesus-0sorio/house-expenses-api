package com.houseexpenses.data.repository

import android.content.Context
import androidx.work.*
import com.houseexpenses.data.local.AppDatabase
import com.houseexpenses.data.local.entity.ExpenseEntity
import com.houseexpenses.data.local.entity.IncomeEntity
import com.houseexpenses.data.local.entity.SettlementPaymentEntity
import com.houseexpenses.data.remote.AuthInterceptor
import com.houseexpenses.data.remote.HouseExpensesApi
import com.houseexpenses.data.remote.SessionManager
import com.houseexpenses.data.remote.dto.*
import com.houseexpenses.worker.SyncWorker
import kotlinx.coroutines.flow.Flow
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

class ExpenseRepository(private val context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val expenseDao = db.expenseDao()
    private val incomeDao = db.incomeDao()
    private val settlementPaymentDao = db.settlementPaymentDao()
    val sessionManager = SessionManager(context)

    val api: HouseExpensesApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(sessionManager))
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(sessionManager.serverUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(HouseExpensesApi::class.java)
    }

    // --- Authentication ---
    suspend fun login(email: String, password: String): Result<UserDto> {
        return try {
            val response = api.login(LoginRequestDto(email, password))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                sessionManager.authToken = body.token
                sessionManager.userId = body.user.id
                sessionManager.userName = body.user.name
                sessionManager.userEmail = body.user.email
                sessionManager.activeHouseholdId = body.user.active_household_id
                if (!body.user.active_household_id.isNullOrBlank()) {
                    fetchCurrentHousehold()
                }
                // Sincronizar datos iniciales
                triggerOneTimeSync()
                Result.success(body.user)
            } else {
                Result.failure(Exception("Error al iniciar sesión: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(email: String, password: String, name: String, taxId: String? = null): Result<UserDto> {
        return try {
            val response = api.register(RegisterRequestDto(email, password, name, taxId))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                sessionManager.authToken = body.token
                sessionManager.userId = body.user.id
                sessionManager.userName = body.user.name
                sessionManager.userEmail = body.user.email
                sessionManager.activeHouseholdId = body.user.active_household_id
                if (!body.user.active_household_id.isNullOrBlank()) {
                    fetchCurrentHousehold()
                }
                Result.success(body.user)
            } else {
                Result.failure(Exception("Error al registrarse: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Local Persistence & Online-First Outbox ---
    fun getAllExpenses(): Flow<List<ExpenseEntity>> = expenseDao.getAllExpenses()
    fun getAllIncomes(): Flow<List<IncomeEntity>> = incomeDao.getAllIncomes()
    fun getAllSettlementPayments(): Flow<List<SettlementPaymentEntity>> = settlementPaymentDao.getAllPayments()

    suspend fun createExpense(expense: ExpenseEntity) {
        var sentDirectly = false
        val householdId = if (expense.scope == "HOUSEHOLD") {
            expense.householdId ?: sessionManager.activeHouseholdId
        } else null
        val fullExpense = expense.copy(householdId = householdId)
        try {
            val syncDto = ExpenseSyncDto(
                id = fullExpense.id,
                client_id = fullExpense.id,
                user_id = sessionManager.userId,
                household_id = fullExpense.householdId,
                amount = fullExpense.amount,
                date = fullExpense.date,
                category = fullExpense.category,
                description = fullExpense.description,
                scope = fullExpense.scope,
                payment_status = fullExpense.paymentStatus
            )
            val response = api.syncBatch(SyncRequest(expenses = listOf(syncDto), incomes = emptyList()))
            if (response.isSuccessful) {
                sentDirectly = true
                expenseDao.insertExpense(fullExpense.copy(syncStatus = "SYNCED"))
            }
        } catch (e: Exception) {
            sentDirectly = false
        }

        if (!sentDirectly) {
            // Buffer Outbox: Se guarda pendiente en Room SQLite para sincronización posterior
            expenseDao.insertExpense(fullExpense.copy(syncStatus = "PENDING_SYNC"))
            triggerOneTimeSync()
        }
    }

    suspend fun createIncome(income: IncomeEntity) {
        var sentDirectly = false
        try {
            val syncDto = IncomeSyncDto(
                id = income.id,
                user_id = sessionManager.userId,
                amount = income.amount,
                type = income.type,
                date = income.date,
                description = income.description
            )
            val response = api.syncBatch(SyncRequest(expenses = emptyList(), incomes = listOf(syncDto)))
            if (response.isSuccessful) {
                sentDirectly = true
                incomeDao.insertIncome(income.copy(syncStatus = "SYNCED"))
            }
        } catch (e: Exception) {
            sentDirectly = false
        }

        if (!sentDirectly) {
            incomeDao.insertIncome(income.copy(syncStatus = "PENDING_SYNC"))
            triggerOneTimeSync()
        }
    }

    suspend fun updateExpense(expense: ExpenseEntity) {
        createExpense(expense)
    }

    suspend fun deleteExpense(id: String) {
        expenseDao.deleteExpense(id)
    }

    suspend fun updateIncome(income: IncomeEntity) {
        createIncome(income)
    }

    suspend fun deleteIncome(id: String) {
        incomeDao.deleteIncome(id)
    }

    suspend fun fetchHouseholdBalance(): HouseholdBalanceResponse? {
        return try {
            val response = api.getHouseholdBalance()
            if (response.isSuccessful) response.body() else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun createHousehold(name: String): Result<HouseholdDto> {
        return try {
            val response = api.createHousehold(CreateHouseholdDto(name))
            if (response.isSuccessful && response.body() != null) {
                val household = response.body()!!
                sessionManager.activeHouseholdId = household.id
                sessionManager.householdInviteCode = household.invite_code
                sessionManager.householdName = household.name
                sessionManager.householdBillingCycleDay = household.billing_cycle_day
                sessionManager.isHouseholdAdmin = true
                Result.success(household)
            } else {
                Result.failure(Exception("Error al crear hogar: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun joinHousehold(inviteCode: String): Result<HouseholdDto> {
        return try {
            val response = api.joinHousehold(JoinHouseholdDto(inviteCode.trim().uppercase()))
            if (response.isSuccessful && response.body() != null) {
                val household = response.body()!!
                sessionManager.activeHouseholdId = household.id
                sessionManager.householdInviteCode = household.invite_code
                sessionManager.householdName = household.name
                sessionManager.householdBillingCycleDay = household.billing_cycle_day
                sessionManager.isHouseholdAdmin = household.created_by == sessionManager.userId
                Result.success(household)
            } else {
                Result.failure(Exception("Código de invitación inválido"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getHouseholdMembers(): List<HouseholdMemberDto> {
        return try {
            val response = api.getHouseholdMembers()
            if (response.isSuccessful && response.body() != null) {
                val list = response.body()!!
                val currentMember = list.find { it.user_id == sessionManager.userId }
                if (currentMember != null) {
                    sessionManager.isHouseholdAdmin = currentMember.isAdmin
                }
                list
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun updateMemberSplits(splits: List<MemberSplitDto>): Result<Unit> {
        return try {
            val response = api.updateMemberSplits(splits)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error al actualizar reparto: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteHousehold(): Result<Unit> {
        return try {
            val response = api.deleteHousehold()
            if (response.isSuccessful) {
                sessionManager.clearHousehold()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error al eliminar hogar: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun leaveHousehold(): Result<Unit> {
        return try {
            val response = api.leaveHousehold()
            if (response.isSuccessful) {
                sessionManager.clearHousehold()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error al abandonar hogar: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateBillingCycle(cycleDay: Int): Result<HouseholdDto> {
        return try {
            val response = api.updateBillingCycle(UpdateBillingCycleDto(cycleDay))
            if (response.isSuccessful && response.body() != null) {
                val h = response.body()!!
                sessionManager.householdBillingCycleDay = h.billing_cycle_day
                Result.success(h)
            } else {
                Result.failure(Exception("Error al actualizar ciclo: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCyclesHistory(): List<CycleSummaryDto> {
        return try {
            val response = api.getCyclesHistory()
            if (response.isSuccessful) response.body() ?: emptyList() else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun removeMember(userId: String): Result<Unit> {
        return try {
            val response = api.removeMember(userId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error al expulsar miembro: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCycleDetail(cycleIndex: Int): Result<CycleDetailResponse> {
        return try {
            val response = api.getCycleDetail(cycleIndex)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error al obtener detalle del ciclo: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchCurrentHousehold(): Result<CurrentHouseholdResponseDto> {
        return try {
            val response = api.getCurrentHousehold()
            if (response.isSuccessful && response.body() != null) {
                val h = response.body()!!
                sessionManager.activeHouseholdId = h.id
                sessionManager.householdName = h.name
                sessionManager.householdInviteCode = h.invite_code
                sessionManager.householdBillingCycleDay = h.billing_cycle_day
                sessionManager.isHouseholdAdmin = h.is_admin
                Result.success(h)
            } else {
                Result.failure(Exception("Error al consultar hogar: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createSettlementPayment(
        receiverUserId: String,
        amount: Double,
        channel: String = "Transferencia",
        notes: String = ""
    ): Result<SettlementPaymentEntity> {
        val householdId = sessionManager.activeHouseholdId
            ?: return Result.failure(Exception("No perteneces a ningún hogar activo"))
        val id = UUID.randomUUID().toString()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val entity = SettlementPaymentEntity(
            id = id,
            householdId = householdId,
            payerUserId = sessionManager.userId,
            receiverUserId = receiverUserId,
            payerName = sessionManager.userName,
            amount = amount,
            channel = channel,
            date = today,
            notes = notes,
            syncStatus = "PENDING"
        )

        var sentDirectly = false
        try {
            val req = CreateSettlementPaymentDto(
                client_id = id,
                receiver_user_id = receiverUserId,
                amount = amount,
                channel = channel,
                date = today,
                notes = notes
            )
            val resp = api.createSettlementPayment(req)
            if (resp.isSuccessful && resp.body() != null) {
                sentDirectly = true
                val resDto = resp.body()!!
                val synced = entity.copy(
                    id = resDto.id,
                    payerName = resDto.payer?.name ?: entity.payerName,
                    receiverName = resDto.receiver?.name ?: "",
                    syncStatus = "SYNCED"
                )
                settlementPaymentDao.insertPayment(synced)
                return Result.success(synced)
            }
        } catch (_: Exception) {
            sentDirectly = false
        }

        if (!sentDirectly) {
            settlementPaymentDao.insertPayment(entity.copy(syncStatus = "PENDING_SYNC"))
            triggerOneTimeSync()
        }
        return Result.success(entity)
    }

    // --- Recurring Debts / Subscriptions ---
    suspend fun createRecurringDebt(
        name: String,
        monthlyAmount: Double,
        dueDay: Int,
        scope: String = "PERSONAL",
        remainingInstallments: Int = 0
    ): Result<RecurringDebtDto> {
        return try {
            val response = api.createDebt(
                CreateDebtRequestDto(
                    name = name,
                    monthly_amount = monthlyAmount,
                    due_day = dueDay,
                    remaining_installments = remainingInstallments,
                    scope = scope
                )
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error al crear compromiso recurrente: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listRecurringDebts(): Result<List<RecurringDebtDto>> {
        return try {
            val response = api.listDebts()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error al consultar compromisos recurrentes: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateRecurringDebt(
        id: String,
        name: String,
        monthlyAmount: Double,
        dueDay: Int,
        scope: String = "PERSONAL",
        remainingInstallments: Int = 0
    ): Result<RecurringDebtDto> {
        return try {
            val response = api.updateDebt(
                id = id,
                request = CreateDebtRequestDto(
                    name = name,
                    monthly_amount = monthlyAmount,
                    due_day = dueDay,
                    remaining_installments = remainingInstallments,
                    scope = scope
                )
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error al actualizar compromiso recurrente: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteRecurringDebt(id: String): Result<Unit> {
        return try {
            val response = api.deleteDebt(id)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error al eliminar compromiso recurrente: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Batch Sync & Remote Pull ---
    suspend fun performSync(): Boolean {
        val pendingExpenses = expenseDao.getPendingSyncExpenses()
        val pendingIncomes = incomeDao.getPendingSyncIncomes()
        val pendingPayments = settlementPaymentDao.getPendingSyncPayments()

        var pushSuccess = true
        if (pendingExpenses.isNotEmpty() || pendingIncomes.isNotEmpty() || pendingPayments.isNotEmpty()) {
            val expenseDtos = pendingExpenses.map { exp ->
                ExpenseSyncDto(
                    id = exp.id,
                    client_id = exp.id,
                    user_id = sessionManager.userId,
                    household_id = if (exp.scope == "HOUSEHOLD") (exp.householdId ?: sessionManager.activeHouseholdId) else null,
                    amount = exp.amount,
                    date = exp.date,
                    category = exp.category,
                    description = exp.description,
                    scope = exp.scope,
                    payment_status = exp.paymentStatus
                )
            }

            val incomeDtos = pendingIncomes.map { inc ->
                IncomeSyncDto(
                    id = inc.id,
                    client_id = inc.id,
                    user_id = sessionManager.userId,
                    amount = inc.amount,
                    type = inc.type,
                    date = inc.date,
                    description = inc.description
                )
            }

            val paymentDtos = pendingPayments.map { p ->
                PaymentSyncDto(
                    id = p.id,
                    client_id = p.id,
                    receiver_user_id = p.receiverUserId,
                    amount = p.amount,
                    channel = p.channel,
                    date = p.date,
                    notes = p.notes
                )
            }

            try {
                val response = api.syncBatch(SyncRequest(expenses = expenseDtos, incomes = incomeDtos, payments = paymentDtos))
                if (response.isSuccessful) {
                    val body = response.body()
                    val syncedExpenseIds = body?.syncedExpenseIds ?: emptyList()
                    for (id in syncedExpenseIds) {
                        expenseDao.updateSyncStatus(id, "SYNCED")
                    }
                    val syncedIncomeIds = body?.syncedIncomeIds ?: emptyList()
                    for (id in syncedIncomeIds) {
                        incomeDao.updateSyncStatus(id, "SYNCED")
                    }
                    val syncedPaymentIds = body?.syncedPaymentIds ?: emptyList()
                    for (id in syncedPaymentIds) {
                        settlementPaymentDao.updateSyncStatus(id, "SYNCED")
                    }
                } else {
                    pushSuccess = false
                }
            } catch (e: Exception) {
                pushSuccess = false
            }
        }

        // Pull remoto: descargar transacciones del backend
        try {
            val remoteExpensesResp = api.listExpenses()
            if (remoteExpensesResp.isSuccessful && remoteExpensesResp.body() != null) {
                for (dto in remoteExpensesResp.body()!!) {
                    expenseDao.insertExpense(
                        ExpenseEntity(
                            id = dto.id,
                            userId = if (dto.user_id.isNotBlank()) dto.user_id else sessionManager.userId,
                            householdId = dto.household_id,
                            amount = dto.amount,
                            date = dto.date,
                            category = dto.category,
                            description = dto.description,
                            scope = dto.scope,
                            paymentStatus = dto.payment_status,
                            syncStatus = "SYNCED"
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        // Pull remoto: descargar ingresos del backend
        try {
            val remoteIncomesResp = api.listIncomes()
            if (remoteIncomesResp.isSuccessful && remoteIncomesResp.body() != null) {
                for (dto in remoteIncomesResp.body()!!) {
                    incomeDao.insertIncome(
                        IncomeEntity(
                            id = dto.id,
                            userId = if (dto.user_id.isNotBlank()) dto.user_id else sessionManager.userId,
                            amount = dto.amount,
                            type = dto.type,
                            date = dto.date,
                            description = dto.description,
                            syncStatus = "SYNCED"
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        // Pull remoto: descargar pagos del hogar
        if (!sessionManager.activeHouseholdId.isNullOrBlank()) {
            try {
                val remotePaymentsResp = api.getSettlementPayments(100, 0)
                if (remotePaymentsResp.isSuccessful && remotePaymentsResp.body() != null) {
                    for (dto in remotePaymentsResp.body()!!) {
                        settlementPaymentDao.insertPayment(
                            SettlementPaymentEntity(
                                id = dto.id,
                                householdId = dto.household_id,
                                payerUserId = dto.payer_user_id,
                                receiverUserId = dto.receiver_user_id,
                                payerName = dto.payer?.name ?: "",
                                receiverName = dto.receiver?.name ?: "",
                                amount = dto.amount,
                                channel = dto.channel,
                                date = dto.date,
                                notes = dto.notes,
                                syncStatus = "SYNCED"
                            )
                        )
                    }
                }
            } catch (_: Exception) {}
        }

        return pushSuccess
    }

    fun triggerOneTimeSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncWork = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "ExpenseSyncWork",
            ExistingWorkPolicy.REPLACE,
            syncWork
        )
    }
}
