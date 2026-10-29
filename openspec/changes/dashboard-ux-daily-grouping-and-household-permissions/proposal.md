# Proposal: Métricas del Dashboard, Agrupación Diaria de Historial, Gastos del Día en Flujo de Caja y Permisos de Edición en Hogar

## 1. Contexto y Justificación
A partir del uso de la aplicación móvil Android y la retroalimentación de experiencia de usuario, se identificaron varios puntos clave de optimización visual, consistencia de datos, formato de moneda y reglas de permisos:

1. **Desglose de Gastos en Pantalla de Inicio (`DashboardScreen`)**:
   - La tarjeta principal del mes actual muestra el monto global acumulado, pero el usuario no puede distinguir de inmediato cuánto corresponde a **gastos personales** y cuánto a **gastos del hogar**.
   - El texto informativo `"✓ Todo al día"` resulta irrelevante y redundante cuando no hay pendientes de sincronización.
   - En la tarjeta de **Balance del Hogar**, el texto de liquidación muestra valores con decimales técnicos o formatos no adaptados a Colombia (ej. `"Roomie Demo le debe $20000.00 COP a Jesus Osorio."` o `$70000,00`), cuando en pesos colombianos los montos deben expresarse con separador de miles y sin decimales (ej. `$ 20.000` o `$ 100.000`).

2. **Transacciones del Mes en Inicio (`DashboardScreen`)**:
   - Actualmente se mezclan transacciones de todos los integrantes y de meses anteriores bajo el título "Transacciones Recientes".
   - En la pantalla de inicio el encabezado debe renombrarse a **"Transacciones del Mes"** y solo deben visualizarse **las transacciones propias del usuario autenticado**, restringidas al **mes actual**, y con fecha limpia en formato estricto `YYYY-MM-DD` (sin horas, minutos, segundos ni huso horario). Las transacciones globales del hogar pertenecen a la pantalla de Hogar.

3. **Espaciado Inferior Excesivo en Pantallas**:
   - Al deslizar hasta el final de `DashboardScreen`, `ExpensesListScreen`, `CashFlowScreen` y `HouseholdScreen`, queda un espacio en blanco excesivo (`Spacer(80.dp)`), generando una sensación visual de vacío o corte abrupto. Debe reducirse a un espaciado equilibrado (~16-24 dp).

4. **Agrupación Diaria en Historial de Gastos (`ExpensesListScreen`)**:
   - La lista agrupa gastos por `exp.date`. Dado que los registros contienen timestamps completos con horas distintas (ej. `2026-09-05 10:00:00+00`), cada gasto se agrupa en un bloque diario separado, repitiendo el título de fecha para cada transacción del mismo día.
   - Debe agruparse por fecha normalizada (`YYYY-MM-DD`) para que los gastos del mismo día compartan un único encabezado diario con el total del día y sus compras debajo.
   - En el modal de filtros avanzados, el campo de *"O Valor Específico"* resulta confuso e innecesario, debiendo removerse para mantener los filtros limpios y funcionales (rango mínimo y máximo).

5. **Detalle Diario en Flujo de Caja (`CashFlowScreen`)**:
   - La sección de *"Gasto Diario Realizado"* agrupa días históricos en lugar de listar los gastos individuales realizados **en el día actual** (o fecha seleccionada).
   - Debe mostrar los gastos específicos de ese día, cada uno con su descripción/nombre de compra, monto en COP y fecha en formato `YYYY-MM-DD`.

6. **Permisos de Edición en Hogar (`HouseholdScreen`)**:
   - Actualmente cualquier integrante puede hacer clic en cualquier gasto compartido del hogar y modificarlo o eliminarlo mediante `EditExpenseDialog`.
   - Por seguridad y consistencia financiera, **el usuario solo debe poder modificar o eliminar sus propios gastos del hogar** (`exp.userId == currentUserId`). Los gastos registrados por otros roomies deben ser de solo lectura.

7. **Texto de Botón de Creación y Soporte de Gastos Recurrentes / Suscripciones / Arriendo**:
   - En el menú de acciones de `MainContainerScreen.kt`, el botón indica `"Nuevo Gasto Manual"`; debe simplificarse a `"Nuevo Gasto"`, removiendo la palabra "Manual".
   - Debe permitirse registrar **gastos recurrentes mensuales** (suscripciones de streaming, cuota mensual de arriendo, servicios fijos periódicos):
     - En `AddExpenseScreen.kt`, incluir la opción para marcar el gasto como recurrente mensual con su respectivo día de cobro/corte (`due_day` del 1 al 31).
     - Al activarse, se registra tanto el gasto puntual como el compromiso recurrente mensual (`recurring_debts`).
     - En `CashFlowScreen.kt`, desplegar la sección de compromisos fijos y suscripciones con la opción de gestionarlos y visualizarlos claramente.

---

## 2. Alcance de los Cambios

### A. Pantalla de Inicio (`DashboardScreen.kt`) y Contenedor Principal (`MainContainerScreen.kt`)
- **Acción Principal**:
  - En `MainContainerScreen.kt`, actualizar el texto de `"Nuevo Gasto Manual"` a `"Nuevo Gasto"`.
- **Desglose en Tarjeta de Mes Actual**:
  - Mostrar el total general del mes.
  - Añadir dos indicadores o bloques claros:
    - **Gastos Personales**: suma de gastos del mes con `scope == "PERSONAL"` del usuario.
    - **Gastos del Hogar**: suma de gastos del mes con `scope == "HOUSEHOLD"`.
  - Ocultar/remover el texto `"✓ Todo al día"` (conservando solo el estado de sincronización pendiente si `pendingSync > 0`).
- **Formato COP en Balance del Hogar**:
  - Formatear el texto de liquidación para que los montos se muestren en miles con estilo colombiano (ej. `$ 20.000`, `$ 100.000`) sin `.00 COP`.
- **Transacciones del Mes Propias**:
  - Cambiar el título de la sección de `"Transacciones Recientes"` a `"Transacciones del Mes"`.
  - Filtrar transacciones para mostrar únicamente las de `exp.userId == currentUserId` pertenecientes al mes en curso (`exp.date.startsWith(currentMonthKey)`).
  - Limpiar la fecha de cada transacción a `YYYY-MM-DD` (ej. `2026-09-04`).
- **Ajuste de Margen Inferior**:
  - Reducir el espaciador final a 16-24 dp.

### B. Historial de Gastos (`ExpensesListScreen.kt`)
- **Agrupación Diaria Normalizada**:
  - Agrupar los gastos usando solo la porción de fecha `YYYY-MM-DD` (`exp.date.take(10)`).
  - Mostrar un solo encabezado por día con la suma total del día y todas las tarjetas de gastos de ese día contenidas debajo.
- **Filtros Avanzados**:
  - Eliminar el campo `exactAmountFilter` ("O Valor Específico") del estado, de la UI del `ModalBottomSheet` y de la función de filtrado.
- **Ajuste de Margen Inferior**:
  - Reducir espaciador final a 16-24 dp.

### C. Flujo de Caja (`CashFlowScreen.kt`) y Registro de Gastos Recurrentes (`AddExpenseScreen.kt`)
- **Desglose de Gastos del Día**:
  - En la sección inferior, en lugar de una lista agregada de fechas anteriores, listar los gastos individuales realizados en el día (`exp.date.take(10) == todayDate`).
  - Cada elemento debe mostrar el nombre/descripción de la compra, categoría, fecha `YYYY-MM-DD` y monto en COP.
  - Si no hay gastos en el día, mostrar mensaje descriptivo amigable.
- **Soporte de Gastos Recurrentes y Suscripciones**:
  - En `AddExpenseScreen.kt`: checkbox *"¿Es un gasto recurrente mensual? (Arriendo, suscripciones, etc.)"* con selector numérico de día de cobro (1 al 31).
  - En `CashFlowScreen.kt`: sección de *"Compromisos Recurrentes y Suscripciones"* listando arriendo, suscripciones y servicios periódicos del usuario y del hogar, con diálogo rápido para añadir nuevos gastos recurrentes.
- **Ajuste de Margen Inferior**:
  - Reducir espaciador final a 16-24 dp.

### D. Pantalla de Hogar (`HouseholdScreen.kt`) y Backend Go
- **Restricción de Edición**:
  - En la lista de gastos del hogar, verificar si `exp.userId == currentUserId`.
  - Solo permitir abrir el diálogo de edición si el gasto le pertenece al usuario actual.
  - Para gastos de otros usuarios, deshabilitar el clic de edición y opcionalmente mostrar una etiqueta o indicador sutil de solo lectura.
- **Ajuste de Margen Inferior**:
  - Reducir espaciador final a 16-24 dp.
- **Backend Go (`household_usecase.go` y `expense_usecase.go`)**:
  - Ajustar la generación de `settlementText` para dar formato entero en miles o compatible sin decimales `.00`.

---

## 3. Criterios de Aceptación
1. En el Dashboard, la tarjeta del mes desglosa claramente los montos de "Gastos Personales" y "Gastos del Hogar", y ya no aparece el texto "Todo al día".
2. En la tarjeta de Balance del Hogar, la frase de deuda muestra los números formateados con separador de miles (ej. `$ 20.000` o `$ 100.000`), sin decimales `.00`.
3. En la sección "Transacciones del Mes" del Dashboard, el título indica "Transacciones del Mes" y solo aparecen los gastos propios del usuario del mes actual, con fecha en formato `YYYY-MM-DD`.
4. En el Historial de Gastos, todas las compras del mismo día se agrupan bajo un único título de fecha.
5. En el modal de filtros avanzados, no existe el campo de valor específico; solo filtros de rango, categoría, mes y fechas.
6. En Flujo de Caja, se listan los gastos individuales del día actual mostrando el nombre de la compra, fecha `YYYY-MM-DD` y monto.
7. En la pantalla de Hogar, un usuario no puede hacer clic ni abrir el diálogo de edición/eliminación sobre gastos registrados por otros miembros del hogar.
8. Ninguna de las 4 pantallas presenta espacios en blanco desproporcionados al final del scroll.
9. En el menú de acciones principales, el botón indica "Nuevo Gasto" sin la palabra "Manual".
10. El usuario puede marcar un gasto como recurrente mensual (arriendo, suscripciones, servicios fijos) indicando su día de corte, y gestionarlo en la sección de compromisos recurrentes del flujo de caja.
