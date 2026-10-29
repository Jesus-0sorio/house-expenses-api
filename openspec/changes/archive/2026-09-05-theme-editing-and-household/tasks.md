## 1. Tema Dinámico (Dark / Light Theme) & Jerarquía Tipográfica
- [x] 1.1 Actualizar Theme.kt: configurar DarkColorScheme y LightColorScheme con paletas legibles en Material 3 y tipografías consistentes.
- [x] 1.2 Agregar preferencia de tema en SessionManager.kt (isDarkMode: Boolean) y exponer estado reactivo en AuthViewModel o MainContainerScreen.
- [x] 1.3 Agregar botón selector de tema (Sol / Luna) en la TopBar de DashboardScreen.kt y MainContainerScreen.kt.
- [x] 1.4 Refactorizar colores fijos (Color.White, Color(0xFF1E293B)) en DashboardScreen, ExpensesListScreen, CashFlowScreen y HouseholdScreen para usar MaterialTheme.colorScheme.surface, onSurface, background, etc., asegurando 100% de legibilidad en modo claro y oscuro.

## 2. Edición de Gastos e Ingresos
- [x] 2.1 Implementar EditExpenseDialog.kt (o modo edición en AddExpenseScreen): permitir cambiar monto (con formateo COP), descripción, categoría y ámbito (Personal vs Hogar).
- [x] 2.2 Agregar método updateExpense en ExpenseDao, ExpenseRepository y ExpenseViewModel con sincronización inmediata o outbox.
- [x] 2.3 Permitir clic en cualquier gasto en ExpensesListScreen.kt y DashboardScreen.kt para abrir la edición.
- [x] 2.4 Implementar EditIncomeDialog.kt en CashFlowScreen.kt y agregar updateIncome en IncomeDao, ExpenseRepository y ExpenseViewModel.

## 3. Creación y Vinculación de Hogares (Código & Deep Link)
- [x] 3.1 Agregar DTOs y endpoints de hogar en HouseExpensesApi.kt (createHousehold, joinHousehold, getMembers).
- [x] 3.2 Actualizar HouseholdScreen.kt para usuarios sin hogar: mostrar tarjetas interactivas Crear Nuevo Hogar y Unirse con Código.
- [x] 3.3 En HouseholdScreen.kt para usuarios con hogar: mostrar código de invitación, botón para copiar y botón para compartir enlace de invitación.
- [x] 3.4 Configurar IntentFilter en AndroidManifest.xml para Deep Links (houseexpenses://household/join?code=... y https://houseexpenses.app/join/...).
- [x] 3.5 En MainActivity.kt, capturar el intent de Deep Link y abrir el diálogo de confirmación para unirse al hogar con el código pre-cargado.
