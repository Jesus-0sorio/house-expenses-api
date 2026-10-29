## 1. Corrección de Código (8 Caracteres) y Navbar Integrado
- [x] 1.1 Actualizar campo de código en HouseholdScreen.kt para admitir 8 caracteres alfanuméricos (etiqueta, placeholder y validación `length == 8`).
- [x] 1.2 Rediseñar NavigationBar en MainContainerScreen.kt: integrar el botón central `+` como slot central armónico sin FloatingActionButton flotante superpuesto.

## 2. Dashboard y Filtros Avanzados en Historial de Gastos
- [x] 2.1 Actualizar DashboardScreen.kt para calcular y mostrar el total de gastos del mes en curso (`YYYY-MM`) con el nombre del mes.
- [x] 2.2 Agregar barra de búsqueda reactiva por nombre o descripción de gasto en ExpensesListScreen.kt.
- [x] 2.3 Implementar panel de filtros avanzados en ExpensesListScreen.kt:
  - Filtro por mes (Mes actual, Mes anterior, Todos).
  - Filtro por categoría.
  - Filtro por rango de fechas (Desde - Hasta).
  - Filtro por monto (Monto exacto o rango Mínimo - Máximo en COP).
  - Botón para limpiar todos los filtros activos.

## 3. Backend: Reparto Variable, Ciclos de Facturación y Eliminación de Hogar
- [x] 3.1 Actualizar modelos de dominio `Household` (`BillingCycleDay`, `CreatedBy`) y `HouseholdMember` (`Role`) en `domain/household.go`.
- [x] 3.2 Implementar endpoints en `household_handler.go` y `household_usecase.go`:
  - `PUT /households/members/splits` para asignar porcentajes individuales validando suma exacta de 100%.
  - `DELETE /households` para eliminar el hogar por el admin y desvincular a todos los miembros.
  - `POST /households/leave` para que un miembro abandone el hogar.
  - `PUT /households/cycle` para configurar el día de corte (ej. día 29).
  - `GET /households/cycles` para listar el historial de ciclos y su estado (`PENDING` / `SETTLED`).
- [x] 3.3 Actualizar `CalculateHouseholdBalance` en `expense_usecase.go` para calcular la participación con base en el `SplitPercentage` real de cada miembro y dentro de las fechas del ciclo activo.

## 4. Frontend Android: Integrantes, Reparto, Ciclos y Gestión del Hogar
- [x] 4.1 Agregar DTOs y llamadas API en `HouseExpensesApi.kt` (`updateSplits`, `deleteHousehold`, `leaveHousehold`, `updateCycle`, `getCycles`).
- [x] 4.2 Exponer métodos y flujos reactivos en `ExpenseRepository.kt` y `ExpenseViewModel.kt`.
- [x] 4.3 Actualizar `HouseholdScreen.kt`:
  - Visualizar la lista de integrantes del hogar con su nombre, email, rol y porcentaje asignado.
  - Diálogo para que el admin configure los porcentajes de reparto de cada miembro con validación de suma 100%.
  - Configurar día de corte del ciclo y mostrar el período activo (ej. *29 Ago - 28 Sep*).
  - Acciones de "Eliminar Hogar" (admin) y "Abandonar Hogar" (miembro) con diálogos de confirmación.
  - Sección de historial de ciclos anteriores con badges de estado "Pendiente" y "Saldado".
