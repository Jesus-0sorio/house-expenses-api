package com.houseexpenses.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.houseexpenses.data.local.entity.ExpenseEntity
import com.houseexpenses.data.local.entity.SettlementPaymentEntity
import com.houseexpenses.data.remote.dto.CycleDetailResponse
import com.houseexpenses.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {
    val repository = ExpenseRepository(application)
    val sessionManager = repository.sessionManager

    val allExpenses: StateFlow<List<ExpenseEntity>> = repository.getAllExpenses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allIncomes: StateFlow<List<com.houseexpenses.data.local.entity.IncomeEntity>> = repository.getAllIncomes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settlementPayments: StateFlow<List<SettlementPaymentEntity>> = repository.getAllSettlementPayments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingSyncCount: StateFlow<Int> = combine(allExpenses, allIncomes, settlementPayments) { exp, inc, pay ->
        exp.count { it.syncStatus != "SYNCED" } +
        inc.count { it.syncStatus != "SYNCED" } +
        pay.count { it.syncStatus != "SYNCED" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalAmount: StateFlow<Double> = allExpenses.map { list ->
        list.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private val _isDarkMode = MutableStateFlow(sessionManager.isDarkMode)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleTheme() {
        val next = !_isDarkMode.value
        sessionManager.isDarkMode = next
        _isDarkMode.value = next
    }

    private val _hasHousehold = MutableStateFlow(!sessionManager.activeHouseholdId.isNullOrBlank())
    val hasHousehold: StateFlow<Boolean> = _hasHousehold.asStateFlow()

    private val _householdInviteCode = MutableStateFlow(sessionManager.householdInviteCode ?: "")
    val householdInviteCode: StateFlow<String> = _householdInviteCode.asStateFlow()

    private val _householdName = MutableStateFlow(sessionManager.householdName ?: "")
    val householdName: StateFlow<String> = _householdName.asStateFlow()

    private val _isHouseholdAdmin = MutableStateFlow(sessionManager.isHouseholdAdmin)
    val isHouseholdAdmin: StateFlow<Boolean> = _isHouseholdAdmin.asStateFlow()

    private val _billingCycleDay = MutableStateFlow(sessionManager.householdBillingCycleDay)
    val billingCycleDay: StateFlow<Int> = _billingCycleDay.asStateFlow()

    private val _householdMembers = MutableStateFlow<List<com.houseexpenses.data.remote.dto.HouseholdMemberDto>>(emptyList())
    val householdMembers: StateFlow<List<com.houseexpenses.data.remote.dto.HouseholdMemberDto>> = _householdMembers.asStateFlow()

    private val _householdCycles = MutableStateFlow<List<com.houseexpenses.data.remote.dto.CycleSummaryDto>>(emptyList())
    val householdCycles: StateFlow<List<com.houseexpenses.data.remote.dto.CycleSummaryDto>> = _householdCycles.asStateFlow()

    private val _householdBalance = MutableStateFlow<com.houseexpenses.data.remote.dto.HouseholdBalanceResponse?>(null)
    val householdBalance: StateFlow<com.houseexpenses.data.remote.dto.HouseholdBalanceResponse?> = _householdBalance.asStateFlow()

    fun loadHouseholdBalance() {
        viewModelScope.launch {
            val hasH = !sessionManager.activeHouseholdId.isNullOrBlank()
            _hasHousehold.value = hasH
            if (hasH) {
                // Rehidratar sesión del hogar desde el backend primero
                repository.fetchCurrentHousehold()
                _householdInviteCode.value = sessionManager.householdInviteCode ?: ""
                _householdName.value = sessionManager.householdName ?: ""
                _billingCycleDay.value = sessionManager.householdBillingCycleDay
                _isHouseholdAdmin.value = sessionManager.isHouseholdAdmin

                repository.performSync()
                val balance = repository.fetchHouseholdBalance()
                _householdBalance.value = balance
                val members = repository.getHouseholdMembers()
                _householdMembers.value = members
                _isHouseholdAdmin.value = sessionManager.isHouseholdAdmin
                val cycles = repository.getCyclesHistory()
                _householdCycles.value = cycles
            } else {
                _householdInviteCode.value = ""
                _householdName.value = ""
                _householdBalance.value = null
                _householdMembers.value = emptyList()
                _householdCycles.value = emptyList()
            }
        }
    }

    fun createSettlementPayment(
        receiverUserId: String,
        amount: Double,
        channel: String = "Transferencia",
        notes: String = "",
        onSuccess: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val res = repository.createSettlementPayment(receiverUserId, amount, channel, notes)
            res.fold(
                onSuccess = {
                    repository.performSync()
                    loadHouseholdBalance()
                    onSuccess?.invoke()
                },
                onFailure = { e ->
                    onError?.invoke(e.message ?: "Error al registrar pago")
                }
            )
        }
    }

    fun createExpense(
        description: String,
        amount: Double,
        category: String,
        scope: String,
        date: String
    ) {
        viewModelScope.launch {
            val hId = if (scope == "HOUSEHOLD") sessionManager.activeHouseholdId else null
            val entity = ExpenseEntity(
                id = UUID.randomUUID().toString(),
                userId = sessionManager.userId,
                householdId = hId,
                amount = amount,
                date = date,
                category = category,
                description = description,
                scope = scope,
                syncStatus = "PENDING"
            )
            repository.createExpense(entity)
            repository.performSync()
            loadHouseholdBalance()
        }
    }

    fun createIncome(
        source: String,
        amount: Double,
        type: String,
        date: String
    ) {
        viewModelScope.launch {
            val entity = com.houseexpenses.data.local.entity.IncomeEntity(
                id = UUID.randomUUID().toString(),
                userId = sessionManager.userId,
                amount = amount,
                type = type,
                date = date,
                description = source,
                syncStatus = "PENDING"
            )
            repository.createIncome(entity)
            repository.performSync()
        }
    }

    fun updateExpense(
        id: String,
        description: String,
        amount: Double,
        category: String,
        scope: String,
        date: String
    ) {
        viewModelScope.launch {
            val hId = if (scope == "HOUSEHOLD") sessionManager.activeHouseholdId else null
            val entity = ExpenseEntity(
                id = id,
                userId = sessionManager.userId,
                householdId = hId,
                amount = amount,
                date = date,
                category = category,
                description = description,
                scope = scope,
                syncStatus = "PENDING"
            )
            repository.updateExpense(entity)
            repository.performSync()
            loadHouseholdBalance()
        }
    }

    fun deleteExpense(id: String) {
        viewModelScope.launch {
            repository.deleteExpense(id)
            repository.performSync()
            loadHouseholdBalance()
        }
    }

    fun updateIncome(
        id: String,
        source: String,
        amount: Double,
        type: String,
        date: String
    ) {
        viewModelScope.launch {
            val entity = com.houseexpenses.data.local.entity.IncomeEntity(
                id = id,
                userId = sessionManager.userId,
                amount = amount,
                type = type,
                date = date,
                description = source,
                syncStatus = "PENDING"
            )
            repository.updateIncome(entity)
        }
    }

    fun deleteIncome(id: String) {
        viewModelScope.launch {
            repository.deleteIncome(id)
        }
    }

    fun createHousehold(name: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val res = repository.createHousehold(name)
            res.fold(
                onSuccess = { h ->
                    _hasHousehold.value = true
                    _householdInviteCode.value = h.invite_code
                    _householdName.value = h.name
                    _billingCycleDay.value = h.billing_cycle_day
                    _isHouseholdAdmin.value = true
                    loadHouseholdBalance()
                    onSuccess(h.invite_code)
                },
                onFailure = { e -> onError(e.message ?: "Error al crear hogar") }
            )
        }
    }

    fun joinHousehold(inviteCode: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val res = repository.joinHousehold(inviteCode)
            res.fold(
                onSuccess = { h ->
                    _hasHousehold.value = true
                    _householdInviteCode.value = h.invite_code
                    _householdName.value = h.name
                    _billingCycleDay.value = h.billing_cycle_day
                    loadHouseholdBalance()
                    onSuccess()
                },
                onFailure = { e -> onError(e.message ?: "Error al unirse al hogar") }
            )
        }
    }

    fun updateSplits(
        splits: List<com.houseexpenses.data.remote.dto.MemberSplitDto>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.updateMemberSplits(splits)
            res.fold(
                onSuccess = {
                    loadHouseholdBalance()
                    onSuccess()
                },
                onFailure = { e -> onError(e.message ?: "Error al actualizar reparto") }
            )
        }
    }

    fun updateBillingCycle(
        day: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.updateBillingCycle(day)
            res.fold(
                onSuccess = { h ->
                    _billingCycleDay.value = h.billing_cycle_day
                    loadHouseholdBalance()
                    onSuccess()
                },
                onFailure = { e -> onError(e.message ?: "Error al configurar ciclo") }
            )
        }
    }

    fun deleteHousehold(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.deleteHousehold()
            res.fold(
                onSuccess = {
                    _hasHousehold.value = false
                    _householdInviteCode.value = ""
                    _householdName.value = ""
                    _householdMembers.value = emptyList()
                    _householdCycles.value = emptyList()
                    _householdBalance.value = null
                    onSuccess()
                },
                onFailure = { e -> onError(e.message ?: "Error al eliminar hogar") }
            )
        }
    }

    fun leaveHousehold(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.leaveHousehold()
            res.fold(
                onSuccess = {
                    _hasHousehold.value = false
                    _householdInviteCode.value = ""
                    _householdName.value = ""
                    _householdMembers.value = emptyList()
                    _householdCycles.value = emptyList()
                    _householdBalance.value = null
                    onSuccess()
                },
                onFailure = { e -> onError(e.message ?: "Error al abandonar hogar") }
            )
        }
    }

    fun removeMember(
        userId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.removeMember(userId)
            res.fold(
                onSuccess = {
                    loadHouseholdBalance()
                    onSuccess()
                },
                onFailure = { e -> onError(e.message ?: "Error al expulsar integrante") }
            )
        }
    }

    fun getCycleDetail(
        cycleIndex: Int,
        onSuccess: (CycleDetailResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.getCycleDetail(cycleIndex)
            res.fold(
                onSuccess = { detail -> onSuccess(detail) },
                onFailure = { e -> onError(e.message ?: "Error al cargar detalle del ciclo") }
            )
        }
    }

    private val _recurringDebts = MutableStateFlow<List<com.houseexpenses.data.remote.dto.RecurringDebtDto>>(emptyList())
    val recurringDebts: StateFlow<List<com.houseexpenses.data.remote.dto.RecurringDebtDto>> = _recurringDebts.asStateFlow()

    fun loadRecurringDebts() {
        viewModelScope.launch {
            val res = repository.listRecurringDebts()
            res.fold(
                onSuccess = { _recurringDebts.value = it },
                onFailure = { /* ignore */ }
            )
        }
    }

    fun createRecurringDebt(
        name: String,
        amount: Double,
        dueDay: Int,
        scope: String = "PERSONAL",
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val res = repository.createRecurringDebt(
                name = name,
                monthlyAmount = amount,
                dueDay = dueDay,
                scope = scope
            )
            res.fold(
                onSuccess = {
                    loadRecurringDebts()
                    onSuccess()
                },
                onFailure = { e ->
                    onError(e.message ?: "Error al registrar compromiso recurrente")
                }
            )
        }
    }

    fun updateRecurringDebt(
        id: String,
        name: String,
        amount: Double,
        dueDay: Int,
        scope: String = "PERSONAL",
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val res = repository.updateRecurringDebt(
                id = id,
                name = name,
                monthlyAmount = amount,
                dueDay = dueDay,
                scope = scope
            )
            res.fold(
                onSuccess = {
                    loadRecurringDebts()
                    onSuccess()
                },
                onFailure = { e ->
                    onError(e.message ?: "Error al actualizar compromiso recurrente")
                }
            )
        }
    }

    fun deleteRecurringDebt(
        id: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val res = repository.deleteRecurringDebt(id)
            res.fold(
                onSuccess = {
                    loadRecurringDebts()
                    onSuccess()
                },
                onFailure = { e ->
                    onError(e.message ?: "Error al eliminar compromiso recurrente")
                }
            )
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            repository.performSync()
            loadHouseholdBalance()
            loadRecurringDebts()
        }
    }
}
