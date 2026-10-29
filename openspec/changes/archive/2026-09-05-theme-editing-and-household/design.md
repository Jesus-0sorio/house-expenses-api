# Design: Modo Oscuro/Claro, Edición y Gestión de Hogares

## 1. Arquitectura de Tema Dinámico (Material 3)
- `Theme.kt`:
  - `DarkColorScheme`: Primario `EmeraldPrimary` (`#10B981`), Secundario `PurpleAccent` (`#8B5CF6`), Fondo `#0F172A`, Superficie `#1E293B`, Texto `#F8FAFC`.
  - `LightColorScheme`: Primario `EmeraldDark` (`#059669`), Secundario `PurpleAccent` (`#7C3AED`), Fondo `#F8FAFC`, Superficie `#FFFFFF`, Texto `#0F172A`.
  - Reemplazo de colores duros (`Color(0xFF1E293B)`, `Color.White`) en tarjetas y textos por `MaterialTheme.colorScheme.*` para que los contrastes sean siempre óptimos y legibles.
- `SessionManager`:
  - Almacena `isDarkMode: Boolean` o `themeMode: String` ("DARK", "LIGHT", "SYSTEM").
- `MainContainerScreen` / `DashboardScreen`:
  - Botón de cambio de tema en la barra superior (icono Sol / Luna) que alterna el estado en tiempo real.

## 2. Flujo de Edición de Gastos e Ingresos
- **Gastos**:
  - Al hacer clic en un gasto de la lista, se abre `EditExpenseDialog` o se navega a `add_expense` en modo edición con los datos cargados.
  - El usuario puede modificar descripción, monto (con `CopCurrencyVisualTransformation`), categoría y ámbito (`Personal` vs `Hogar`).
  - Al confirmar: `ExpenseViewModel.updateExpense(entity)` actualiza Room SQLite con `syncStatus = "PENDING"` y despacha inmediatamente a `api.syncBatch(...)`.
- **Ingresos**:
  - Al hacer clic en un ingreso en `CashFlowScreen`, se abre `EditIncomeDialog` cargando el concepto, monto y tipo.
  - Al confirmar: `ExpenseViewModel.updateIncome(entity)` actualiza Room SQLite y sincroniza.

## 3. Gestión de Hogares & Deep Links
- **Llamadas al Backend**:
  - `HouseExpensesApi`:
    - `@POST("households") suspend fun createHousehold(@Body req: CreateHouseholdDto): Response<HouseholdDto>`
    - `@POST("households/join") suspend fun joinHousehold(@Body req: JoinHouseholdDto): Response<HouseholdDto>`
    - `@GET("households/members") suspend fun getHouseholdMembers(): Response<List<HouseholdMemberDto>>`
- **UI en `HouseholdScreen.kt`**:
  - **Sin Hogar Activo**:
    - Tarjeta para "Crear Nuevo Hogar" (campo nombre: ej. "Apartamento 302").
    - Tarjeta para "Unirse con Código" (campo código de 6 caracteres).
  - **Con Hogar Activo**:
    - Muestra nombre del hogar, balance 50/50 y código de invitación con botones "Copiar Código" y "Compartir Enlace".
- **Deep Linking**:
  - Esquema URL: `houseexpenses://household/join?code={code}` y enlace universal `https://houseexpenses.app/join/{code}` configurados en `AndroidManifest.xml`.
  - `MainActivity` detecta el intent al abrirse y redirige al flujo de unirse con el código pre-llenado.
