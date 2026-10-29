# Design: Ciclos de Hogar, Repartición Variable, Filtros Avanzados y Navbar Integrado

## 1. Arquitectura del Backend (Go + PostgreSQL)

### A. Modelos de Dominio y Persistencia
- **`Household` ([`domain/household.go`](file:///c:/Users/Jesus%20Osorio/projects/house-expenses/internal/core/domain/household.go)):**
  - Agregar campos:
    - `BillingCycleDay int`: Día del mes en que inicia el ciclo (ej. 29). Por defecto: 1.
    - `CreatedBy uuid.UUID`: ID del creador/administrador del hogar.
- **`HouseholdMember` ([`domain/household.go`](file:///c:/Users/Jesus%20Osorio/projects/house-expenses/internal/core/domain/household.go)):**
  - Agregar campo:
    - `Role string`: `"ADMIN"` para el creador y `"MEMBER"` para los demás.
    - `SplitPercentage float64`: Ya existente en la entidad, se utiliza activamente en los cálculos en lugar del 50/50 hardcodeado.

### B. Endpoints de la API ([`household_handler.go`](file:///c:/Users/Jesus%20Osorio/projects/house-expenses/internal/adapters/handlers/household_handler.go))
1. `PUT /households/members/splits`:
   - Payload: `[ { "user_id": "...", "split_percentage": 60.0 }, { "user_id": "...", "split_percentage": 40.0 } ]`
   - Validación: La suma de porcentajes debe ser exactamente 100.0%. Solo el admin puede ejecutarlo.
2. `DELETE /households`:
   - Elimina el hogar activo del usuario autenticado (si es admin).
   - Limpia en cascada `active_household_id = NULL` para todos los miembros.
3. `POST /households/leave`:
   - Desvincula al usuario miembro del hogar actual y limpia su `active_household_id`.
4. `PUT /households/cycle`:
   - Payload: `{ "billing_cycle_day": 29 }`
   - Actualiza el día de corte del ciclo mensual.
5. `GET /households/cycles`:
   - Devuelve la lista de ciclos con fechas de inicio y fin, gasto total acumulado y estado (`PENDING` / `SETTLED`).

### C. Lógica de Balance Asimétrico ([`expense_usecase.go`](file:///c:/Users/Jesus%20Osorio/projects/house-expenses/internal/core/usecase/expense_usecase.go))
- Para cada miembro $i$:
  $$\text{ExpectedShare}_i = \text{TotalHousehold} \times \left(\frac{\text{SplitPercentage}_i}{100.0}\right)$$
  $$\text{NetBalance}_i = \text{Paid}_i - \text{ExpectedShare}_i$$
- Si $\text{NetBalance}_i > 0$, el usuario es acreedor (le deben).
- Si $\text{NetBalance}_i < 0$, el usuario es deudor (debe).
- Liquidación multidireccional cuando hay 3 o más personas, emparejando los saldos pendientes de forma ordenada.

---

## 2. Arquitectura de la App Android (Jetpack Compose)

### A. Corrección de Invitación por Código (8 Caracteres)
- En [`HouseholdScreen.kt`](file:///c:/Users/Jesus%20Osorio/projects/house-expenses/apps/mobile-android/app/src/main/java/com/houseexpenses/ui/screens/HouseholdScreen.kt):
  - Actualizar `if (input.length <= 8)` y `placeholder = "Ej. 7FB908CB"`.
  - Etiqueta: `"Código de Invitación (8 caracteres)"`.
  - Habilitar botón de unirse cuando `length == 8`.

### B. Navbar con Botón `+` Integrado
- En [`MainContainerScreen.kt`](file:///c:/Users/Jesus%20Osorio/projects/house-expenses/apps/mobile-android/app/src/main/java/com/houseexpenses/ui/screens/MainContainerScreen.kt):
  - Eliminar el `FloatingActionButton` flotante con offset desalineado.
  - Implementar una barra de navegación simétrica de 5 ítems:
    1. `Inicio` (Índice 0)
    2. `Gastos` (Índice 1)
    3. `(+) Registrar` (Índice 2, botón central destacado con contenedor esmeralda circular integrado)
    4. `Flujo` (Índice 3)
    5. `Hogar` (Índice 4)
  - Al tocar el ítem central `(+)`, despliega inmediatamente el `ModalBottomSheet` para escanear QR o agregar gasto manual sin alterar el tab seleccionado.

### C. Métricas del Mes Actual en Dashboard
- En [`DashboardScreen.kt`](file:///c:/Users/Jesus%20Osorio/projects/house-expenses/apps/mobile-android/app/src/main/java/com/houseexpenses/ui/screens/DashboardScreen.kt):
  - Calcular el acumulado mensual:
    ```kotlin
    val currentMonthKey = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    val currentMonthTotal = expenses.filter { it.date.startsWith(currentMonthKey) }.sumOf { it.amount }
    ```
  - Cambiar el texto de la tarjeta a: `"Gastos de ${nombreMesActual}"` (ej. *"Gastos de Septiembre"*).

### D. Filtros y Búsqueda Avanzada en Historial de Gastos
- En [`ExpensesListScreen.kt`](file:///c:/Users/Jesus%20Osorio/projects/house-expenses/apps/mobile-android/app/src/main/java/com/houseexpenses/ui/screens/ExpensesListScreen.kt):
  - Barra de búsqueda de texto superior para filtrar por descripción o nombre de tienda en tiempo real.
  - Fila de chips de filtro y botón de "Filtros Avanzados" que despliega un BottomSheet con:
    1. **Filtro por Mes**: Selector (Mes actual, Mes anterior, Todos).
    2. **Filtro por Categoría**: Selector de chips (Supermercado, Restaurantes, Servicios, Transporte, etc.).
    3. **Filtro por Rango de Fechas**: Campos Desde y Hasta (`YYYY-MM-DD`).
    4. **Filtro por Valor**: Monto exacto o Rango (Mínimo / Máximo con formateo COP).
  - Contador de filtros aplicados y botón para "Limpiar Filtros".

### E. Integrantes, Repartición Variable y Ciclos en Hogar
- En [`HouseholdScreen.kt`](file:///c:/Users/Jesus%20Osorio/projects/house-expenses/apps/mobile-android/app/src/main/java/com/houseexpenses/ui/screens/HouseholdScreen.kt):
  - **Lista de Integrantes**: Muestra cada miembro con su nombre, rol (`Admin` / `Miembro`) y su porcentaje asignado.
  - **Diálogo para Editar Reparto**: Permite al admin ajustar los porcentajes de cada persona mediante campos numéricos/sliders, validando que el total sume 100%.
  - **Configuración del Ciclo de Corte**: Campo para seleccionar el día de corte (ej. 29) y visualización del rango de fechas del ciclo activo (ej. *29 Ago - 28 Sep*).
  - **Eliminación / Salida del Hogar**:
    - Si es Admin: Botón "Eliminar Hogar" con diálogo destructivo.
    - Si es Miembro: Botón "Abandonar Hogar".
  - **Historial de Ciclos del Hogar**: Lista de períodos anteriores con total del período, deudas saldadas y badge `Pendiente` (naranja) o `Saldado` (verde).
