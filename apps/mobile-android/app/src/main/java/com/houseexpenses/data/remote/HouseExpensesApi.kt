package com.houseexpenses.data.remote

import com.houseexpenses.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface HouseExpensesApi {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequestDto): Response<AuthResponseDto>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequestDto): Response<AuthResponseDto>

    @POST("sync")
    suspend fun syncBatch(@Body request: SyncRequest): Response<SyncResponse>

    @POST("invoices/lookup-dian")
    suspend fun lookupDianInvoice(@Body request: DianLookupRequest): Response<DianInvoiceResponse>

    @GET("households/balance")
    suspend fun getHouseholdBalance(): Response<HouseholdBalanceResponse>

    @GET("expenses")
    suspend fun listExpenses(): Response<List<ExpenseSyncDto>>

    @GET("incomes")
    suspend fun listIncomes(): Response<List<IncomeSyncDto>>

    @POST("households")
    suspend fun createHousehold(@Body request: CreateHouseholdDto): Response<HouseholdDto>

    @POST("households/join")
    suspend fun joinHousehold(@Body request: JoinHouseholdDto): Response<HouseholdDto>

    @GET("households/members")
    suspend fun getHouseholdMembers(): Response<List<HouseholdMemberDto>>

    @PUT("households/members/splits")
    suspend fun updateMemberSplits(@Body splits: List<MemberSplitDto>): Response<Map<String, String>>

    @DELETE("households")
    suspend fun deleteHousehold(): Response<Map<String, String>>

    @POST("households/leave")
    suspend fun leaveHousehold(): Response<Map<String, String>>

    @PUT("households/cycle")
    suspend fun updateBillingCycle(@Body request: UpdateBillingCycleDto): Response<HouseholdDto>

    @GET("households/cycles")
    suspend fun getCyclesHistory(): Response<List<CycleSummaryDto>>

    @DELETE("households/members/{user_id}")
    suspend fun removeMember(@Path("user_id") userId: String): Response<Map<String, String>>

    @GET("households/cycles/{cycle_index}")
    suspend fun getCycleDetail(@Path("cycle_index") cycleIndex: Int): Response<CycleDetailResponse>

    @GET("households/current")
    suspend fun getCurrentHousehold(): Response<CurrentHouseholdResponseDto>

    @POST("households/payments")
    suspend fun createSettlementPayment(@Body request: CreateSettlementPaymentDto): Response<SettlementPaymentDto>

    @GET("households/payments")
    suspend fun getSettlementPayments(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): Response<List<SettlementPaymentDto>>

    @POST("finance/debts")
    suspend fun createDebt(@Body request: CreateDebtRequestDto): Response<RecurringDebtDto>

    @GET("finance/debts")
    suspend fun listDebts(): Response<List<RecurringDebtDto>>

    @PUT("finance/debts/{id}")
    suspend fun updateDebt(
        @Path("id") id: String,
        @Body request: CreateDebtRequestDto
    ): Response<RecurringDebtDto>

    @DELETE("finance/debts/{id}")
    suspend fun deleteDebt(@Path("id") id: String): Response<Map<String, String>>
}
