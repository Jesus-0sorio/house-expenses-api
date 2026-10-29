# Tasks: Visibilidad de Integrantes para Admin, Gastos Totales del Hogar y Registro de Pagos de Liquidación Entre Roomies

## 1. Backend Go: Entidad `SettlementPayment`, Endpoints y Balance Compensado
- [x] 1.1 Declarar el modelo de dominio `SettlementPayment` en `internal/core/domain/household.go` y agregarlo al AutoMigrate de GORM en la inicialización de la base de datos.
- [x] 1.2 Implementar los métodos de repositorio para pagos (`CreatePayment`, `FindByHouseholdAndPeriod`) en `ports/repositories.go` y `internal/adapters/repository/household_repository.go`.
- [x] 1.3 Desarrollar los casos de uso en `household_usecase.go`:
  - `CreateSettlementPayment(ctx, householdID, payerID, input)`
  - `GetSettlementPayments(ctx, householdID, start, end)`
  - `GetCurrentHousehold(ctx, userID)` retornando metadatos completos del hogar, rol y miembros.
- [x] 1.4 Modificar `CalculateHouseholdBalance` en `expense_usecase.go` para incorporar los pagos de liquidación del ciclo en el cálculo del saldo neto (`net_balance`), garantizando que los pagos no inflen el `total_household` de compras.
- [x] 1.5 Extender `SyncBatch` en `sync_usecase.go` para aceptar y procesar `payments: []SyncPaymentItem`.
- [x] 1.6 Registrar las rutas REST en `household_handler.go`:
  - `POST /households/payments`
  - `GET /households/payments`
  - `GET /households/current`
- [x] 1.7 Escribir pruebas unitarias en `internal/core/usecase/usecase_test.go` y validar que `go test ./...` pase con 0 errores.

## 2. Frontend Android: Persistencia Local de Pagos y Corrección Outbox en Room
- [x] 2.1 Corregir en `ExpenseDao.kt` la consulta de sincronización pendiente: cambiar a `@Query("SELECT * FROM expenses WHERE syncStatus != 'SYNCED'")` para procesar tanto `"PENDING"` como `"PENDING_SYNC"`.
- [x] 2.2 Crear la entidad Room `SettlementPaymentEntity` y `SettlementPaymentDao` en `com.houseexpenses.data.local`, añadiéndolas a `AppDatabase`.
- [x] 2.3 Añadir los DTOs de pago (`SettlementPaymentDto`, `CreateSettlementPaymentDto`, `CurrentHouseholdResponseDto`) en `SyncBatchDto.kt` y los métodos en `HouseExpensesApi.kt`.
- [x] 2.4 Actualizar `ExpenseRepository.kt`:
  - Implementar `createSettlementPayment(receiverId, amount, channel, notes)`.
  - Implementar `fetchCurrentHousehold()` y actualizar sesión (nombre, invite code, admin status, billing cycle day).
  - En `performSync()`, procesar pagos pendientes y descargar pagos remotos del ciclo.
- [x] 2.5 Extender `ExpenseViewModel.kt` con el flujo de pagos `settlementPayments: StateFlow<List<SettlementPaymentEntity>>` y función `createSettlementPayment(...)`.

## 3. Frontend Android: UI de Liquidación y Registro de Pagos en `HouseholdScreen.kt`
- [x] 3.1 Actualizar `SettlementPaymentDialog` para incluir el selector de integrante destinatario (*"¿A quién le vas a pagar?"*), preseleccionando al roomie acreedor principal y restringiendo canales a "Transferencia" y "Efectivo".
- [x] 3.2 Añadir en `HouseholdScreen.kt` la sección visual de **"Registro de Pagos"** (o "Historial de Liquidaciones del Ciclo") mostrando quién le pagó a quién, el monto en COP y el canal.
- [x] 3.3 Rehidratar automáticamente la sesión del admin y miembros al entrar a `HouseholdScreen` ejecutando `fetchCurrentHousehold()` y `performSync()` para garantizar que los integrantes y gastos totales siempre se visualicen.

## 4. Verificación y Pruebas
- [x] 4.1 Ejecutar `go test -v -count=1 ./...` verificando la cobertura del nuevo flujo de liquidación y balance compensado.
- [x] 4.2 Validar la compilación del código Kotlin en Android Studio / Gradle.
