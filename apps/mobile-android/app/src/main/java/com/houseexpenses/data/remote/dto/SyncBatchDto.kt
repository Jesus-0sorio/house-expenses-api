package com.houseexpenses.data.remote.dto

data class LoginRequestDto(
    val email: String,
    val password: String
)

data class RegisterRequestDto(
    val email: String,
    val password: String,
    val name: String,
    val tax_id: String? = null
)

data class UserDto(
    val id: String,
    val email: String,
    val name: String,
    val tax_id: String? = null,
    val active_household_id: String? = null
)

data class AuthResponseDto(
    val token: String,
    val user: UserDto
)

data class ExpenseSyncDto(
    val id: String,
    val client_id: String = id,
    val user_id: String,
    val household_id: String? = null,
    val amount: Double,
    val date: String,
    val category: String,
    val description: String,
    val scope: String,
    val payment_status: String = "PAID"
)

data class IncomeSyncDto(
    val id: String,
    val client_id: String = id,
    val user_id: String,
    val amount: Double,
    val type: String,
    val date: String,
    val description: String
)

data class PaymentSyncDto(
    val id: String,
    val client_id: String = id,
    val receiver_user_id: String,
    val amount: Double,
    val channel: String = "Transferencia",
    val date: String,
    val notes: String = ""
)

data class SyncRequest(
    val expenses: List<ExpenseSyncDto> = emptyList(),
    val incomes: List<IncomeSyncDto> = emptyList(),
    val payments: List<PaymentSyncDto> = emptyList(),
    val last_synced_at: String? = null
)

data class SyncItemAckDto(
    val client_id: String? = null,
    val server_id: String? = null,
    val status: String = "SYNCED",
    val error: String? = null
)

data class SyncResponse(
    val synced_expenses: List<SyncItemAckDto> = emptyList(),
    val synced_incomes: List<SyncItemAckDto> = emptyList(),
    val synced_payments: List<SyncItemAckDto> = emptyList(),
    val server_time: String = ""
) {
    val syncedExpenseIds: List<String>
        get() = synced_expenses.mapNotNull { it.client_id ?: it.server_id }.filter { it.isNotBlank() }
    val syncedIncomeIds: List<String>
        get() = synced_incomes.mapNotNull { it.client_id ?: it.server_id }.filter { it.isNotBlank() }
    val syncedPaymentIds: List<String>
        get() = synced_payments.mapNotNull { it.client_id ?: it.server_id }.filter { it.isNotBlank() }
}

data class DianLookupRequest(
    val qr_text: String? = null,
    val cufe: String? = null,
    val nit: String? = null
)

data class DianInvoiceResponse(
    val id: String,
    val cufe: String?,
    val issuer_name: String,
    val issuer_nit: String,
    val invoice_number: String,
    val total_amount: Double,
    val tax_amount: Double
)

data class RoomieBalanceDto(
    val user_id: String,
    val user_name: String,
    val split_percentage: Double = 50.0,
    val total_paid: Double = 0.0,
    val expected_share: Double = 0.0,
    val net_balance: Double = 0.0
)

data class HouseholdBalanceResponse(
    val household_id: String,
    val cycle_start: String? = null,
    val cycle_end: String? = null,
    val total_household: Double = 0.0,
    val balances: List<RoomieBalanceDto> = emptyList(),
    val settlement: String = ""
) {
    val settlement_summary: String
        get() = settlement
    val user_paid: Double
        get() = balances.firstOrNull()?.total_paid ?: 0.0
    val roomie_paid: Double
        get() = if (balances.size > 1) balances[1].total_paid else 0.0
    fun getUserPaid(userId: String): Double {
        return balances.find { it.user_id == userId }?.total_paid ?: 0.0
    }
    fun getUserNet(userId: String): Double {
        return balances.find { it.user_id == userId }?.net_balance ?: 0.0
    }
}

data class CycleExpenseItemDto(
    val id: String,
    val date: String,
    val description: String,
    val category: String,
    val amount: Double,
    val payer_name: String,
    val payment_status: String
)

data class CycleDetailResponse(
    val cycle_index: Int,
    val start_date: String,
    val end_date: String,
    val status: String,
    val total_amount: Double,
    val expenses: List<CycleExpenseItemDto> = emptyList(),
    val balances: List<RoomieBalanceDto> = emptyList(),
    val settlement: String = ""
)

data class CreateHouseholdDto(
    val name: String
)

data class JoinHouseholdDto(
    val invite_code: String
)

data class HouseholdDto(
    val id: String,
    val name: String,
    val invite_code: String,
    val created_by: String? = null,
    val billing_cycle_day: Int = 1
)

data class MemberUserDto(
    val id: String? = null,
    val name: String? = null,
    val email: String? = null
)

data class HouseholdMemberDto(
    val id: String? = null,
    val household_id: String? = null,
    val user_id: String,
    val role: String = "MEMBER",
    val split_percentage: Double = 50.0,
    val joined_at: String? = null,
    val user: MemberUserDto? = null
) {
    val displayName: String
        get() = user?.name ?: "Miembro"
    val displayEmail: String
        get() = user?.email ?: ""
    val isAdmin: Boolean
        get() = role.equals("ADMIN", ignoreCase = true)
}

data class MemberSplitDto(
    val user_id: String,
    val split_percentage: Double
)

data class UpdateBillingCycleDto(
    val billing_cycle_day: Int
)

data class CycleSummaryDto(
    val cycle_index: Int,
    val start_date: String,
    val end_date: String,
    val total_amount: Double,
    val status: String
)

data class SettlementPaymentDto(
    val id: String,
    val household_id: String,
    val payer_user_id: String,
    val receiver_user_id: String,
    val amount: Double,
    val channel: String = "Transferencia",
    val date: String,
    val notes: String = "",
    val created_at: String? = null,
    val payer: MemberUserDto? = null,
    val receiver: MemberUserDto? = null
)

data class CreateSettlementPaymentDto(
    val client_id: String? = null,
    val receiver_user_id: String,
    val amount: Double,
    val channel: String = "Transferencia",
    val date: String? = null,
    val notes: String = ""
)

data class CurrentHouseholdResponseDto(
    val id: String,
    val name: String,
    val invite_code: String,
    val role: String,
    val billing_cycle_day: Int = 1,
    val members: List<HouseholdMemberDto> = emptyList(),
    val is_admin: Boolean = false
)

data class CreateDebtRequestDto(
    val name: String,
    val monthly_amount: Double,
    val due_day: Int,
    val remaining_installments: Int = 0,
    val scope: String = "PERSONAL"
)

data class RecurringDebtDto(
    val id: String,
    val user_id: String,
    val name: String,
    val monthly_amount: Double,
    val due_day: Int,
    val remaining_installments: Int = 0,
    val scope: String = "PERSONAL",
    val created_at: String? = null
)
