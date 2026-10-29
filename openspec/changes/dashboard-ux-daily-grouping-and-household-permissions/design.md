# Design: Métricas de Inicio, Agrupación Diaria, Detalle de Flujo de Caja y Restricción de Edición

## 1. Arquitectura y Solución Técnica

### A. Pantalla de Inicio (`DashboardScreen.kt`)
1. **Cálculo reactivo de sub-totales del mes actual**:
   ```kotlin
   val currentMonthPersonal = remember(expenses, currentMonthKey) {
       expenses.filter { it.date.startsWith(currentMonthKey) && it.scope == "PERSONAL" }
           .sumOf { it.amount }
   }
   val currentMonthHousehold = remember(expenses, currentMonthKey) {
       expenses.filter { it.date.startsWith(currentMonthKey) && it.scope == "HOUSEHOLD" }
           .sumOf { it.amount }
   }
   ```
2. **Estructura de la Tarjeta del Mes**:
   - Encabezado: "Gastos de [Mes Año]" y Monto Total Consolidado en tipografía destacada (`28.sp`, negrita).
   - Separador visual y fila con dos sub-tarjetas o columnas simétricas:
     - **Gastos Personales**: Monto formateado con `formatCOP`.
     - **Gastos del Hogar**: Monto formateado con `formatCOP`.
   - Se elimina por completo el texto `"✓ Todo al día"`. Se preserva de forma condicional únicamente la advertencia `"⏳ N pendiente(s) de sincronización"` cuando `pendingSync > 0`.
3. **Formateo de Liquidación en Pesos Colombianos**:
   - Se implementa una función de formateo para el texto de liquidación:
     ```kotlin
     fun formatSettlementText(raw: String, formatCOP: NumberFormat): String {
         val regex = Regex("""\$([0-9]+(?:\.[0-9]+)?)(?:\s*COP)?""")
         return regex.replace(raw) { matchResult ->
             val num = matchResult.groupValues[1].toDoubleOrNull() ?: 0.0
             formatCOP.format(num)
         }
     }
     ```
   - Esto convierte cadenas crudas como `"Roomie Demo le debe $20000.00 COP a Jesus Osorio."` en `"Roomie Demo le debe $ 20.000 a Jesus Osorio."`.
4. **Sección "Transacciones del Mes" y Formato de Fecha**:
   - Cambiar el título en la UI de `"Transacciones Recientes"` a `"Transacciones del Mes"`.
   - Filtrar únicamente transacciones pertenecientes al usuario actual y dentro del mes activo:
     ```kotlin
     val myRecentMonthExpenses = remember(expenses, currentMonthKey) {
         expenses.filter { 
             it.userId == viewModel.sessionManager.userId && 
             it.date.startsWith(currentMonthKey) 
         }.sortedByDescending { it.date }
     }
     ```
   - Fecha de cada transacción: mostrar `exp.date.take(10)` para garantizar un formato limpio `YYYY-MM-DD` sin horas ni timestamps.
5. **Espacio Inferior**:
   - Reemplazar `Spacer(modifier = Modifier.height(80.dp))` por `Spacer(modifier = Modifier.height(20.dp))`.

---

### B. Historial de Gastos (`ExpensesListScreen.kt`)
1. **Solución al Fallo de Agrupación Diaria**:
   - Actualmente `monthExps.groupBy { it.date }` utiliza el timestamp completo, lo que genera grupos independientes para compras registradas a diferentes horas del mismo día.
   - Se normaliza la clave diaria extrayendo estrictamente los primeros 10 caracteres (`YYYY-MM-DD`):
     ```kotlin
     val byDay = monthExps.groupBy { exp ->
         if (exp.date.length >= 10) exp.date.substring(0, 10) else exp.date
     }
     ```
   - Esto consolida automáticamente todas las transacciones del mismo día bajo un único `DayExpensesGroup`. El encabezado diario (`formatDayHeader`) se renderiza una sola vez por día mostrando la suma agregada del día y la lista completa de compras debajo.
2. **Limpieza del Modal de Filtros**:
   - Se elimina la variable de estado `exactAmountFilter` y su campo `OutlinedTextField` en el `ModalBottomSheet`.
   - Se mantiene el filtrado por rango (`minAmountFilter` y `maxAmountFilter`), categoría, mes y fechas.
3. **Espacio Inferior**:
   - Reducir `Spacer(80.dp)` a `Spacer(20.dp)`.

---

### C. Flujo de Caja (`CashFlowScreen.kt`)
1. **Desglose de Gastos del Día Actual**:
   - Se define la lista de compras individuales del día de hoy:
     ```kotlin
     val todayExpenses = remember(expenses, todayDate) {
         expenses.filter { it.date.take(10) == todayDate }
             .sortedByDescending { it.date }
     }
     ```
   - En la sección *"Gastos del Día"*, se itera sobre `todayExpenses` mostrando una tarjeta por cada gasto con:
     - Nombre / descripción de la compra (`exp.description`).
     - Chip o etiqueta de categoría (`exp.category`).
     - Fecha en formato `YYYY-MM-DD` (`exp.date.take(10)`).
     - Monto en formato COP (`formatCOP.format(exp.amount)`).
   - Si `todayExpenses.isEmpty()`, se muestra un mensaje informativo amigable ("No tienes compras registradas hoy ($todayDate)").
2. **Espacio Inferior**:
   - Reducir `Spacer(80.dp)` a `Spacer(20.dp)`.

---

### D. Pantalla de Hogar (`HouseholdScreen.kt`)
1. **Restricción de Edición según Propietario del Gasto**:
   - En el listado de gastos compartidos:
     ```kotlin
     val isMe = exp.userId == viewModel.sessionManager.userId
     Card(
         onClick = { if (isMe) expenseToEdit = exp },
         enabled = isMe,
         modifier = Modifier.fillMaxWidth(),
         ...
     ```
   - Si `!isMe`, la tarjeta no es clickeable para edición, no dispara `EditExpenseDialog` y muestra claramente la autoría (`"Pagado por: $payerName"` con indicador de solo lectura si aplica).
2. **Formato de Fecha**:
   - Mostrar `exp.date.take(10)` en la descripción de fecha de la tarjeta.
3. **Espacio Inferior**:
   - Reducir `Spacer(80.dp)` a `Spacer(20.dp)`.

---

### E. Backend Go (`household_usecase.go` y `expense_usecase.go`)
- En `CalculateHouseholdBalance` (ambos archivos), ajustar la plantilla del mensaje de liquidación para formatear los números sin decimales `.00`:
  ```go
  settlementText = fmt.Sprintf("%s le debe $%.0f COP a %s.", debtor, transfer, creditor)
  ```
- O utilizar un formateador con separador de miles si se prefiere a nivel de servidor, manteniendo compatibilidad con el formateador dinámico de Android.

---

### F. Botón de Creación en `MainContainerScreen.kt`
- En `MainContainerScreen.kt` (línea 218), actualizar:
  ```kotlin
  text = "Nuevo Gasto" // Eliminando el término 'Manual'
  ```

---

### G. Integración de Gastos Recurrentes / Suscripciones / Arriendo
1. **Formulario `AddExpenseScreen.kt`**:
   - Estados:
     ```kotlin
     var isRecurring by remember { mutableStateOf(false) }
     var recurringDueDay by remember { mutableStateOf("5") }
     ```
   - Control interactivo:
     ```kotlin
     Row(verticalAlignment = Alignment.CenterVertically) {
         Checkbox(
             checked = isRecurring,
             onCheckedChange = { isRecurring = it },
             colors = CheckboxDefaults.colors(checkedColor = EmeraldPrimary)
         )
         Text("¿Es un gasto recurrente mensual? (Arriendo, suscripciones, etc.)")
     }
     ```
   - Si `isRecurring == true`, desplegar selector de día del mes (1 al 31).
   - Al guardar, ejecutar `createExpense` para el gasto puntual y `createRecurringDebt` para registrar el pasivo recurrente.
2. **ViewModel y Red (`HouseExpensesApi.kt` y `ExpenseViewModel.kt`)**:
   - Agregar endpoints `createDebt` y `listDebts` en `HouseExpensesApi.kt`.
   - Exponer `recurringDebts: StateFlow<List<RecurringDebtDto>>` en `ExpenseViewModel.kt`.
3. **Flujo de Caja (`CashFlowScreen.kt`)**:
   - Añadir tarjeta y lista de *"Compromisos Recurrentes y Suscripciones"* mostrando nombre, monto mensual, día de cobro y etiqueta de ámbito (Personal / Hogar).
   - Añadir diálogo `AddRecurringDebtDialog` para registrar directamente compromisos recurrentes desde la pantalla de flujo de caja.
