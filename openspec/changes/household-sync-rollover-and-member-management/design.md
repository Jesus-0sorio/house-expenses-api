# Design Document: Sincronización Global de Gastos, Balances Detallados, Rollover de Ciclos y Scroll Infinito

## Architecture & Data Flow

### 1. Sincronización Cruzada de Gastos del Hogar (Multi-Usuario)
**Problema actual:**
El repositorio de gastos únicamente consultaba gastos con `WHERE user_id = ?`. Como consecuencia, los gastos con ámbito `HOUSEHOLD` creados por otros miembros nunca se retornaban en `GET /api/v1/expenses` ni se sincronizaban a los dispositivos móviles de los compañeros de casa.

**Diseño de la solución:**
```
[Android Client Roomie B] 
        │
        ▼ GET /api/v1/expenses
[ExpenseHandler / ExpenseUseCase]
        │ 
        ├─► Consulta usuario actual y su active_household_id
        │
        ▼ ExpenseRepository.FindByUserOrHousehold(userID, activeHouseholdID, limit, offset)
[Postgres Database]
        WHERE user_id = :userID OR (scope = 'HOUSEHOLD' AND household_id = :householdID)
        ORDER BY date DESC, created_at DESC
```
- Se agrega el método `FindByUserOrHousehold` tanto en `ports.ExpenseRepository` como en `repository.ExpenseRepository`.
- Si el usuario no pertenece a ningún hogar (`activeHouseholdID == nil`), se mantiene la consulta de gastos personales exclusivamente.

---

### 2. Auto-Rebalanceo Equitativo a 100.0% en Altas y Bajas
**Problema actual:**
Al unirse un miembro a un hogar, se le asignaba un porcentaje calculado (`100/N`) pero los registros de los integrantes existentes no se actualizaban en la base de datos, provocando que la suma superara el 100%.

**Diseño del algoritmo de rebalanceo:**
```go
func (uc *HouseholdUseCase) rebalanceMembersEquitably(ctx context.Context, householdID uuid.UUID) error {
    members, err := uc.householdRepo.GetMembers(ctx, householdID)
    if err != nil || len(members) == 0 {
        return err
    }
    n := len(members)
    basePct := math.Floor((100.0/float64(n))*100.0) / 100.0
    remainder := math.Round((100.0-(basePct*float64(n)))*100.0) / 100.0

    for i := range members {
        pct := basePct
        if i == 0 {
            pct += remainder // El primer miembro absorbe los centésimos residuales para sumar exactamente 100.00%
        }
        members[i].SplitPercentage = pct
        _ = uc.householdRepo.UpdateMember(ctx, &members[i])
    }
    return nil
}
```
Este helper se ejecuta atómicamente tras:
1. `JoinHousehold`: Se añade el nuevo miembro y se rebalancean todos.
2. `RemoveMember`: Se elimina el miembro indicado y se rebalancean los restantes.
3. `LeaveHousehold`: Se remueve el miembro saliente y se rebalancean los restantes.

---

### 3. Expulsión de Miembros por el Administrador
- **Endpoint**: `DELETE /api/v1/households/members/:user_id`
- **Control de Acceso**:
  - Se verifica que el solicitante (`c.Get("user_id")`) sea miembro con rol `ADMIN` en el hogar activo.
  - Se valida que `targetUserID != requesterID` (el admin no puede expulsarse a sí mismo mediante esta ruta; para disolver el hogar debe usar `DELETE /households`).
- **Efectos Secundarios**:
  - Se remueve el registro de `household_members`.
  - Se actualiza `users.active_household_id = NULL` para el usuario expulsado.
  - Se ejecuta `rebalanceMembersEquitably` para los miembros que permanecen.

---

### 4. Rollover de Deudas y Detalle de Ciclo
- **Detalle de Ciclo (`GET /api/v1/households/cycles/:cycle_index`)**:
  - Acepta el índice del ciclo (0 = activo, 1 = anterior, etc.).
  - Calcula los límites temporales `[start, end]` mediante `CalculateCycleBounds(cycleDay, refDate)`.
  - Consulta los gastos con `FindByHouseholdAndPeriod`.
  - Calcula el aporte pagado de cada miembro, la cuota esperada según sus porcentajes, el saldo neto y las instrucciones de liquidación.
- **Rollover de Saldos No Saldados**:
  - Al calcular el balance del ciclo activo (ciclo 0), el usecase revisa el ciclo inmediatamente anterior (ciclo 1).
  - Si el ciclo 1 tiene estado `PENDING` (no saldado completamente), los saldos deudores pendientes se suman al déficit del deudor en el ciclo actual y se acreditan a los acreedores correspondientes.

---

### 5. Frontend Android: DTO de Balance Corregido y UI Enriquecida
**Contrato DTO:**
```kotlin
data class RoomieBalanceDto(
    val user_id: String,
    val user_name: String,
    val split_percentage: Double,
    val total_paid: Double,
    val expected_share: Double,
    val net_balance: Double
)

data class HouseholdBalanceResponse(
    val household_id: String,
    val cycle_start: String? = null,
    val cycle_end: String? = null,
    val total_household: Double,
    val balances: List<RoomieBalanceDto> = emptyList(),
    val settlement: String
)
```

**Representación en `HouseholdScreen.kt`:**
1. **Tarjeta de Balance General**:
   - Total acumulado del hogar en COP (ej. `Total Compartido: $785.000 COP`).
   - Lista detallada de cada roomie:
     - Nombre + "(Tú)"
     - Aporte pagado: `$230.000 COP`
     - Cuota esperada (50%): `$392.500 COP`
     - Estado neto: Chip verde `A favor +$162.500` o Chip rojo `Debe $162.500`.
   - Caja de liquidación con el texto explicativo: *"Roomie 1 le debe $162.500 COP a Roomie 2."*
2. **Botón de Expulsar en Integrantes**:
   - Icono `Delete` en cada miembro no admin para el usuario administrador.
   - Diálogo modal de confirmación antes de la llamada a la API.
3. **Modal de Detalle de Ciclos Pasados**:
   - Al pulsar una tarjeta en la lista de ciclos, se abre un `ModalBottomSheet` mostrando los gastos registrados en ese mes, quién los pagó y el desglose de liquidación.

---

### 6. Agrupación por Día/Mes y Scroll Infinito en Transacciones
- En [ExpensesListScreen.kt](file:///c:/Users/Jesus%20Osorio/projects/house-expenses/apps/mobile-android/app/src/main/java/com/houseexpenses/ui/screens/ExpensesListScreen.kt):
  - Agrupación en memoria de la lista de gastos por `YearMonth` (ej. *"Septiembre 2026"*) y luego por fecha `YYYY-MM-DD` (ej. *"Hoy, 5 de Septiembre"*, *"Ayer, 4 de Septiembre"*).
  - Paginación dinámica: cuando el usuario se desplaza a los últimos 5 elementos del `LazyColumn`, se solicita la siguiente página (`limit = 50, offset = currentCount`) actualizando fluidamente la base local y la UI sin saltos.
