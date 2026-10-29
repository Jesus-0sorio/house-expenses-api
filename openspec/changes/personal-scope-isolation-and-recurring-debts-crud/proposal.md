# Proposal: Aislamiento de Gastos Propios en Dashboard/Historial y CRUD Completo de Compromisos Recurrentes

## 1. Contexto y Justificación

A partir de la revisión de uso de la aplicación móvil y la experiencia de usuario, se identificaron tres aspectos esenciales de privacidad, claridad visual y gestión financiera:

1. **Etiquetas Redundantes en Transacciones del Mes (`DashboardScreen`)**:
   - En la pantalla de inicio, la sección *"Transacciones del Mes"* ya lista exclusivamente los gastos realizados por el propio usuario en sesión.
   - Sin embargo, para los gastos con ámbito de hogar (`HOUSEHOLD`), la tarjeta dibuja un badge que indica `"Por: Tú"`. Esta etiqueta resulta redundante y añade ruido visual, ya que por definición en esa pantalla solo van los gastos propios del usuario (tanto personales como los que él pagó para el hogar).

2. **Aislamiento de Gastos Propios en el Historial de Gastos (`ExpensesListScreen`)**:
   - Actualmente, la pantalla de *"Historial de Gastos"* consume todos los registros almacenados localmente en SQLite, incluyendo aquellos gastos compartidos del hogar que fueron registrados y pagados por otros integrantes (roomies).
   - El usuario requiere que el Historial de Gastos sea un espacio **estrictamente personal**: solo deben aparecer los gastos efectuados por él mismo (`userId == currentUserId`), ya sean personales o del hogar.
   - La pantalla de **Hogar** (`HouseholdScreen`) debe ser el **único lugar** donde se visualicen los gastos realizados por los demás integrantes de la casa.
   - En consecuencia, en el Historial de Gastos tampoco se requiere mostrar etiquetas de autoría tipo `"Por: Tú"` o `"Por: [Roomie]"`.

3. **Modificación y Eliminación de Gastos Recurrentes / Suscripciones**:
   - Los compromisos fijos y suscripciones (arriendo, internet, Netflix, cuotas) actualmente solo se pueden visualizar y crear de manera unidireccional.
   - No existe la posibilidad de modificar sus valores (por ejemplo, si subió el costo del servicio o cambió la fecha de corte) ni de eliminarlos cuando se cancela una suscripción o finaliza un contrato.
   - Es necesario habilitar la edición (`PUT /finance/debts/:id`) y eliminación (`DELETE /finance/debts/:id`) tanto en el backend Go como en la interfaz de usuario de Android (`CashFlowScreen`).

---

## 2. Alcance de los Cambios

### A. Pantalla de Inicio (`DashboardScreen.kt`)
- Eliminar la etiqueta/badge de autoría (`"Por: Tú"` o `"Por: [Nombre]"`) en las tarjetas de la sección *"Transacciones del Mes"*.
- Conservar únicamente el badge de ámbito (*"Hogar"* o *"Personal"*), la categoría del gasto, la fecha en formato limpio `YYYY-MM-DD` y el monto en COP.

### B. Historial de Gastos (`ExpensesListScreen.kt`)
- Filtrar la lista de transacciones para que contenga **exclusivamente los gastos realizados por el usuario en sesión** (`exp.userId == currentUserId`).
- Excluir del listado, de los totales y de los filtros acumulativos cualquier gasto efectuado por otros integrantes del hogar.
- Eliminar el badge de autoría (`"Por: Tú"` / `"Por: [Nombre]"`), manteniendo solo el badge de ámbito (*"Hogar"* / *"Personal"*), categoría y fecha.
- Garantizar que la pantalla de **Hogar** (`HouseholdScreen.kt`) continúe siendo el único punto de convergencia donde se ven los gastos colectivos de todos los integrantes.

### C. Backend Go (`internal/core/`, `internal/adapters/`)
- En `ports.IncomeRepository`: añadir métodos `FindDebtByID`, `UpdateDebt` y `DeleteDebt`.
- En `GormIncomeRepository`: implementar las operaciones GORM para actualizar y eliminar registros de la tabla `recurring_debts`.
- En `CashFlowUseCase`: implementar `UpdateDebt` y `DeleteDebt` asegurando que un usuario solo pueda modificar o eliminar sus propios compromisos recurrentes.
- En `FinanceHandler`: registrar los endpoints HTTP `PUT /api/v1/finance/debts/:id` y `DELETE /api/v1/finance/debts/:id`.

### D. Cliente Android (`data/`, `ui/`)
- En `HouseExpensesApi.kt`: agregar las funciones `@PUT("finance/debts/{id}")` y `@DELETE("finance/debts/{id}")`.
- En `ExpenseRepository.kt`: implementar `updateRecurringDebt` y `deleteRecurringDebt`.
- En `ExpenseViewModel.kt`: exponer las funciones `updateRecurringDebt(...)` y `deleteRecurringDebt(...)`.
- En `CashFlowScreen.kt`:
  - Hacer clickeables las tarjetas de la sección *"Compromisos Recurrentes y Suscripciones"*.
  - Crear el diálogo interactivo `EditRecurringDebtDialog` que permita editar el nombre, monto mensual, día límite de pago (1-31) y ámbito, así como un botón para eliminar el compromiso recurrente con confirmación.

---

## 3. Criterios de Éxito
1. En `DashboardScreen`, ninguna transacción del mes muestra la etiqueta "Por: Tú".
2. En `ExpensesListScreen`, solo se listan gastos propios del usuario autenticado; ningún gasto registrado por roomies aparece en esta pantalla.
3. En `HouseholdScreen`, se mantienen visibles todos los gastos compartidos del hogar de todos los integrantes (con edición permitida solo para los propios).
4. El usuario puede tocar cualquier compromiso recurrente en `CashFlowScreen`, cambiar su monto/día/nombre o eliminarlo, reflejándose inmediatamente en la interfaz y en la base de datos del backend.
5. Todas las pruebas unitarias del backend Go (`go test ./...`) pasan exitosamente con 0 errores.
