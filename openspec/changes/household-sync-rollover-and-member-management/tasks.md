## 1. Backend: Sincronización Cruzada de Gastos del Hogar
- [x] 1.1 Agregar `FindByUserOrHousehold(ctx, userID, householdID, limit, offset)` en `ports.ExpenseRepository` y su implementación en Postgres GORM.
- [x] 1.2 Actualizar `ListExpenses` en `expense_usecase.go` para consultar gastos personales del usuario y gastos compartidos del hogar activo.
- [x] 1.3 Verificar y añadir pruebas unitarias en `usecase_test.go` para validar que los compañeros de hogar ven los gastos compartidos creados por otros miembros.

## 2. Backend: Auto-Rebalanceo, Expulsión de Miembros y Rollover
- [x] 2.1 Implementar `rebalanceMembersEquitably` en `household_usecase.go` y enlazarlo en `JoinHousehold` y `LeaveHousehold` garantizando suma exacta del 100.0%.
- [x] 2.2 Implementar `RemoveMember` en `household_usecase.go` y endpoint `DELETE /api/v1/households/members/:user_id` en `household_handler.go` (autorizado solo para el ADMIN).
- [x] 2.3 Implementar endpoint `GET /api/v1/households/cycles/:cycle_index` para consultar el detalle de gastos y liquidación de un ciclo específico.
- [x] 2.4 Actualizar `CalculateHouseholdBalance` para incorporar arrastre de deudas pendientes (*rollover*) del ciclo anterior al balance activo.

## 3. Frontend Android: DTO de Balance y Visualización Detallada
- [x] 3.1 Actualizar `HouseholdBalanceResponse` e incorporar `RoomieBalanceDto` en `SyncBatchDto.kt` para coincidir exactamente con el JSON de `SettlementSummary`.
- [x] 3.2 Rediseñar la tarjeta de Balance en `HouseholdScreen.kt`:
  - Mostrar Total acumulado del hogar en COP.
  - Mostrar desglose individual por integrante (nombre, total pagado, cuota esperada, saldo neto a favor o en contra).
  - Mostrar texto explicativo de quién le debe a quién.

## 4. Frontend Android: Expulsión de Integrantes y Detalle de Ciclo
- [x] 4.1 Agregar `removeMember(userId)` en `HouseExpensesApi.kt`, `ExpenseRepository.kt` y `ExpenseViewModel.kt`.
- [x] 4.2 Agregar botón de expulsión con diálogo de confirmación destructiva en cada integrante no admin en `HouseholdScreen.kt`.
- [x] 4.3 Agregar `getCycleDetail` en la capa de datos e implementar `CycleDetailBottomSheet` en `HouseholdScreen.kt` al presionar una tarjeta del historial.

## 5. Frontend Android: Agrupación por Mes/Día y Scroll Infinito
- [x] 5.1 Implementar agrupación jerárquica de transacciones por Mes y Día con encabezados estilizados en `ExpensesListScreen.kt`.
- [x] 5.2 Implementar scroll infinito / paginación reactiva en `ExpensesListScreen.kt` detectando el desplazamiento hacia el final de la lista.
