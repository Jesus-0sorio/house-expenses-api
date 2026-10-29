# Design Document: Visibilidad de Integrantes para Admin, Gastos Totales del Hogar y Registro de Pagos de Liquidación Entre Roomies

## Architecture & Data Model

### 1. Modelo de Datos Backend (Go / GORM / PostgreSQL)

Se introduce la nueva entidad `SettlementPayment` en `internal/core/domain/household.go`:
```go
type SettlementPayment struct {
    ID             uuid.UUID `json:"id" gorm:"type:uuid;primaryKey"`
    HouseholdID    uuid.UUID `json:"household_id" gorm:"type:uuid;not null;index"`
    PayerUserID    uuid.UUID `json:"payer_user_id" gorm:"type:uuid;not null;index"`
    ReceiverUserID uuid.UUID `json:"receiver_user_id" gorm:"type:uuid;not null;index"`
    Amount         float64   `json:"amount" gorm:"type:decimal(12,2);not null"`
    Channel        string    `json:"channel" gorm:"type:varchar(50);not null"` // "Transferencia" o "Efectivo"
    Date           time.Time `json:"date" gorm:"not null"`
    Notes          string    `json:"notes" gorm:"type:text"`
    CreatedAt      time.Time `json:"created_at"`

    Payer          *User     `json:"payer,omitempty" gorm:"foreignKey:PayerUserID"`
    Receiver       *User     `json:"receiver,omitempty" gorm:"foreignKey:ReceiverUserID"`
}
```

### 2. Algoritmo de Compensación de Deudas en `CalculateHouseholdBalance`

Para un ciclo de facturación dado con fecha de inicio $S$ y fecha de fin $E$:
1. **Total de Gastos del Hogar**:
   $$T = \sum_{e \in \text{Expenses}, \text{scope} = \text{HOUSEHOLD}} e.\text{Amount}$$
2. **Gastos Pagados por Integrante $u$**:
   $$P_u = \sum_{e \in \text{Expenses}, e.\text{UserID} = u} e.\text{Amount}$$
3. **Cuota Esperada de Integrante $u$**:
   $$C_u = T \times \frac{\text{split\_percentage}_u}{100}$$
4. **Pagos de Liquidación Realizados por $u$ hacia otros en el ciclo**:
   $$S_u = \sum_{p \in \text{Payments}, p.\text{PayerUserID} = u} p.\text{Amount}$$
5. **Pagos de Liquidación Recibidos por $u$ de parte de otros en el ciclo**:
   $$R_u = \sum_{p \in \text{Payments}, p.\text{ReceiverUserID} = u} p.\text{Amount}$$
6. **Saldo Neto Final Compensado de $u$**:
   $$\text{Net}_u = (P_u - C_u) + S_u - R_u + \text{Carryover}_u$$

*Interpretación:*
- Si $\text{Net}_u < 0$: $u$ aún debe $|\text{Net}_u|$.
- Si $\text{Net}_u > 0$: $u$ aún tiene a favor $\text{Net}_u$.
- Si $\text{Net}_u = 0$: Las cuentas están perfectamente saldadas.
- **Importante**: Las transferencias de liquidación $p$ NO alteran $T$ (el total de gastos de la casa permanece inmutable e íntegro).

### 3. Modelo de Datos Local Android (Room SQLite)

En el paquete `com.houseexpenses.data.local.entity`:
```kotlin
@Entity(tableName = "settlement_payments")
data class SettlementPaymentEntity(
    @PrimaryKey val id: String,
    val householdId: String,
    val payerUserId: String,
    val payerName: String,
    val receiverUserId: String,
    val receiverName: String,
    val amount: Double,
    val channel: String, // "Transferencia" o "Efectivo"
    val date: String,
    val notes: String = "",
    val syncStatus: String = "PENDING", // "PENDING", "SYNCED"
    val createdAt: Long = System.currentTimeMillis()
)
```

En `com.houseexpenses.data.local.dao.SettlementPaymentDao`:
- `getAllPayments(): Flow<List<SettlementPaymentEntity>>`
- `getPendingPayments(): List<SettlementPaymentEntity>`
- `insertPayment(payment: SettlementPaymentEntity)`
- `updateSyncStatus(id: String, status: String)`

### 4. Corrección de la Consulta Outbox en `ExpenseDao.kt`

```kotlin
@Query("SELECT * FROM expenses WHERE syncStatus != 'SYNCED'")
suspend fun getPendingSyncExpenses(): List<ExpenseEntity>
```
Con esto se unifica el tratamiento de `"PENDING"` y `"PENDING_SYNC"`.

### 5. Recuperación y Rehidratación de Sesión del Hogar

Al autenticarse (`AuthViewModel.login`) y al abrir `HouseholdScreen`:
- Invocar `repository.fetchCurrentHousehold()` (`GET /api/v1/households/current`).
- Actualizar `sessionManager`:
  - `activeHouseholdId = household.id`
  - `householdName = household.name`
  - `householdInviteCode = household.invite_code`
  - `householdBillingCycleDay = household.billing_cycle_day`
  - `isHouseholdAdmin = (currentUserId == household.created_by || userRole == "ADMIN")`
- Esto garantiza que el Admin mantenga siempre sus privilegios y vista completa del hogar.

### 6. Modal de Pago con Selector de Destinatario

```kotlin
@Composable
fun SettlementPaymentDialog(
    members: List<HouseholdMemberDto>,
    currentUserId: String,
    defaultReceiverId: String?,
    defaultAmount: Double,
    onDismiss: () -> Unit,
    onConfirm: (receiverId: String, channel: String, amount: Double) -> Unit
)
```
- Menú desplegable o botones de selección para elegir al integrante destinatario.
- Validación de que no se pueda pagar a uno mismo.
- Canales exclusivos: "Transferencia" y "Efectivo".
