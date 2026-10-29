# Tasks: Métricas del Dashboard, Agrupación Diaria de Historial, Gastos del Día en Flujo de Caja y Permisos de Edición en Hogar

## 1. Dashboard: Desglose de Gastos, Formato de Moneda y Transacciones del Mes
- [x] 1.1 En `DashboardScreen.kt`, actualizar la tarjeta principal del mes:
  - Calcular los sub-totales `currentMonthPersonal` (gastos personales del mes) y `currentMonthHousehold` (gastos del hogar del mes).
  - Añadir en la tarjeta la visualización separada de "Gastos Personales" y "Gastos del Hogar" con sus respectivos montos en COP.
  - Eliminar el texto `"✓ Todo al día"` (conservar solo el indicador si `pendingSync > 0`).
- [x] 1.2 En `DashboardScreen.kt`, aplicar formateo de moneda colombiana en la tarjeta de Balance del Hogar:
  - Formatear el texto de `settlement` para que los montos se muestren en miles sin decimales técnicos (ej. `$ 20.000` o `$ 100.000` en lugar de `$20000.00 COP` o `$70000,00`).
- [x] 1.3 En `DashboardScreen.kt`, ajustar la sección de transacciones:
  - Cambiar el título de la sección de `"Transacciones Recientes"` a `"Transacciones del Mes"`.
  - Filtrar para mostrar únicamente las transacciones del usuario actual (`exp.userId == currentUserId`).
  - Restringir la lista a las transacciones del mes en curso (`exp.date.startsWith(currentMonthKey)`).
  - Formatear la fecha en cada tarjeta a formato estricto `YYYY-MM-DD` (`exp.date.take(10)`).
- [x] 1.4 En `DashboardScreen.kt`, reducir el espaciador inferior final de 80.dp a 20.dp.

## 2. Historial de Gastos: Agrupación Diaria y Limpieza de Filtros
- [x] 2.1 En `ExpensesListScreen.kt`, corregir la agrupación de transacciones por día:
  - Modificar `byDay` para que agrupe por los primeros 10 caracteres (`exp.date.take(10)`), asegurando que todas las compras de un mismo día se reúnan bajo un único encabezado diario con el total del día.
- [x] 2.2 En `ExpensesListScreen.kt`, remover el filtro de valor específico:
  - Eliminar `exactAmountFilter` del estado, del modal `ModalBottomSheet` de filtros avanzados y de la función de filtrado.
- [x] 2.3 En `ExpensesListScreen.kt`, reducir el espaciador inferior final de 80.dp a 20.dp.

## 3. Flujo de Caja: Desglose Diario con Detalle de Compras
- [x] 3.1 En `CashFlowScreen.kt`, actualizar la sección de gastos diarios:
  - Sustituir la agregación histórica por la lista de gastos individuales realizados en el día de hoy (`exp.date.take(10) == todayDate`).
  - Mostrar en cada tarjeta el nombre/descripción de la compra (`exp.description`), categoría, fecha `YYYY-MM-DD` y monto en COP.
  - Presentar un mensaje amigable cuando no haya gastos en el día.
- [x] 3.2 En `CashFlowScreen.kt`, reducir el espaciador inferior final de 80.dp a 20.dp.

## 4. Hogar: Restricción de Edición a Gastos Propios y Formato
- [x] 4.1 En `HouseholdScreen.kt`, restringir la edición de gastos compartidos:
  - Verificar si `exp.userId == viewModel.sessionManager.userId`.
  - Habilitar el clic y la apertura de `EditExpenseDialog` exclusivamente si el gasto le pertenece al usuario actual.
  - Asegurar que los gastos de otros integrantes sean de solo lectura para evitar modificaciones o eliminaciones accidentales.
- [x] 4.2 En `HouseholdScreen.kt`, formatear la fecha a `YYYY-MM-DD` (`exp.date.take(10)`) en las tarjetas de gastos del hogar.
- [x] 4.3 En `HouseholdScreen.kt`, reducir el espaciador inferior final de 80.dp a 20.dp.

## 5. Acciones Principales y Gastos Recurrentes / Suscripciones
- [x] 5.1 En `MainContainerScreen.kt`, actualizar el texto de la opción flotante `"Nuevo Gasto Manual"` a `"Nuevo Gasto"`.
- [x] 5.2 En `AddExpenseScreen.kt`, implementar opción de gasto recurrente mensual:
  - Añadir checkbox *"¿Es un gasto recurrente mensual? (Arriendo, suscripción, servicio)"*.
  - Mostrar campo de día de cobro/corte mensual (1 al 31).
  - Al guardar, registrar el gasto del mes y llamar a `createRecurringDebt` para asentar el compromiso fijo.
- [x] 5.3 En `HouseExpensesApi.kt` y `ExpenseViewModel.kt`, integrar los endpoints `createDebt` y `listDebts` para gestionar compromisos fijos mensuales.
- [x] 5.4 En `CashFlowScreen.kt`, añadir sección interactiva de *"Compromisos Recurrentes y Suscripciones"* y diálogo `AddRecurringDebtDialog` para registrar y visualizar gastos periódicos (arriendo, servicios fijos, streaming).

## 6. Backend Go y Verificación
- [x] 6.1 En `internal/core/usecase/household_usecase.go` y `internal/core/usecase/expense_usecase.go`, ajustar el formato del texto de liquidación para montos enteros sin `.00`.
- [x] 6.2 Ejecutar `go test -v -count=1 ./...` y verificar que todas las pruebas pasen con 0 errores.
