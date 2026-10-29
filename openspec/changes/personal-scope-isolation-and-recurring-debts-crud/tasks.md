# Tasks: Aislamiento de Gastos Propios en Dashboard/Historial y CRUD de Compromisos Recurrentes

## 1. Dashboard: Limpieza de Etiquetas de Autoría
- [x] 1.1 En `DashboardScreen.kt`, en la sección *"Transacciones del Mes"*, remover el badge condicional `"Por: Tú"` / `"Por: $payerName"`, manteniendo únicamente la insignia de ámbito ("Hogar" / "Personal"), la categoría, la fecha `YYYY-MM-DD` y el valor.

## 2. Historial de Gastos: Aislamiento de Gastos Propios
- [x] 2.1 En `ExpensesListScreen.kt`, filtrar la lista `filteredExpenses` para incluir exclusivamente las transacciones del usuario actual (`exp.userId == currentUserId`).
- [x] 2.2 En `ExpensesListScreen.kt`, remover el badge de autoría `"Por: Tú"` / `"Por: $payerName"` en cada tarjeta de gasto, asegurando una presentación limpia y consistente.
- [x] 2.3 Verificar que la pantalla `HouseholdScreen.kt` permanezca intacta como el único espacio donde se visualizan los gastos colectivos de todos los integrantes de la casa.

## 3. Backend Go: CRUD de Compromisos Recurrentes
- [x] 3.1 En `internal/core/ports/repositories.go`, declarar en `IncomeRepository` los métodos `FindDebtByID`, `UpdateDebt` y `DeleteDebt`.
- [x] 3.2 En `internal/adapters/repository/income_repository.go`, implementar `FindDebtByID`, `UpdateDebt` y `DeleteDebt` usando GORM.
- [x] 3.3 En `internal/core/usecase/cashflow_usecase.go`:
  - Definir `UpdateDebtInput`.
  - Implementar `UpdateDebt(ctx, userID, debtID, input)` con validación de propiedad (`debt.UserID == userID`).
  - Implementar `DeleteDebt(ctx, userID, debtID)` con validación de propiedad.
- [x] 3.4 En `internal/adapters/handlers/finance_handler.go`:
  - Registrar rutas `PUT /finance/debts/:id` y `DELETE /finance/debts/:id`.
  - Implementar `UpdateDebt` y `DeleteDebt` handlers.
- [x] 3.5 En `internal/core/usecase/cashflow_usecase_test.go`, agregar pruebas unitarias para actualización y eliminación de deudas recurrentes y verificar con `go test -v -count=1 ./...`.

## 4. Cliente Android: Capa de Datos y ViewModel para Deudas Recurrentes
- [x] 4.1 En `HouseExpensesApi.kt`, declarar los métodos `@PUT("finance/debts/{id}")` y `@DELETE("finance/debts/{id}")`.
- [x] 4.2 En `ExpenseRepository.kt`, implementar los métodos `updateRecurringDebt` y `deleteRecurringDebt`.
- [x] 4.3 En `ExpenseViewModel.kt`, exponer los métodos `updateRecurringDebt(...)` y `deleteRecurringDebt(...)`, refrescando reactivamente la lista `loadRecurringDebts()`.

## 5. Cliente Android: Diálogo de Edición y Eliminación en Flujo de Caja
- [x] 5.1 En `CashFlowScreen.kt`, agregar estado para la deuda en edición (`debtToEdit`) y habilitar el clic en las tarjetas de compromisos recurrentes.
- [x] 5.2 En `CashFlowScreen.kt`, implementar el composable `EditRecurringDebtDialog` con:
  - Formulario prellenado (nombre, monto en COP, día de corte 1-31, ámbito).
  - Acción de guardar cambios.
  - Acción de eliminar compromiso recurrente con confirmación.
- [x] 5.3 En `CashFlowScreen.kt`, conectar el diálogo con las funciones `viewModel.updateRecurringDebt` y `viewModel.deleteRecurringDebt`.

## 6. Verificación Final
- [x] 6.1 Ejecutar `go test -v -count=1 ./...` y asegurar que todas las pruebas pasen con 0 errores.
- [x] 6.2 Verificar visualmente y mediante pruebas que el Historial y el Dashboard sean estrictamente personales, mientras que el Hogar preserve la visibilidad colectiva.
