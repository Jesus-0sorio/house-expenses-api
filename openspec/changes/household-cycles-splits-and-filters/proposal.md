# Proposal: Ciclos de Hogar, Repartición Variable, Filtros Avanzados y Mejoras de UX

## Why
La aplicación móvil y el backend requieren resolver discrepancias funcionales y habilitar capacidades esenciales para la convivencia y el control financiero real en Colombia:
1. **Bug en Vinculación por Código**: La UI y validación limitaban la entrada a 6 caracteres con texto "6 letras", mientras que el backend genera códigos de 8 caracteres alfanuméricos (ej. `7FB908CB`). Esto imposibilitaba vincularse manualmente.
2. **Repartición Variable e Integrantes del Hogar**: La lógica asumía un reparto rígido 50/50 para 2 personas. Al unirse 3 o más personas, o en convivencias con acuerdos asimétricos (ej. 60/40, 70/30, 40/30/30), el administrador o creador del hogar debe poder ver a todos los miembros y configurar los porcentajes de aporte individuales (validando que sumen 100%).
3. **Eliminación y Salida del Hogar**: Una vez creado un hogar no existía mecanismo para eliminarlo o para que un integrante se desligue, dejando al usuario bloqueado en un hogar vacío o de prueba.
4. **Métricas Focalizadas en el Mes Actual**: En la pantalla de Inicio (Dashboard), el monto acumulado sumaba todos los gastos históricos de la cuenta en lugar de reflejar el gasto real del mes en curso.
5. **Historial de Gastos con Búsqueda y Filtros Flexibles**: La lista de gastos solo permitía filtrar por ámbito (Personal/Hogar), careciendo de búsqueda por concepto, filtro por mes, selección por categoría, rangos de fecha y rangos de valor (monto exacto o mínimo/máximo).
6. **Navbar Limpio e Integrado**: El botón flotante `+` se encontraba superpuesto y desfasado sobre la barra de navegación inferior. Debe integrarse orgánicamente en el centro del Navbar.
7. **Ciclos de Facturación y Liquidaciones del Hogar**: Los gastos compartidos operan comúnmente con fechas de corte específicas (por ejemplo, del 29 de un mes al 29 del siguiente). El hogar debe permitir configurar este ciclo, registrar gastos bajo el ciclo activo, consultar el historial por ciclo/mes y visualizar si el período quedó pendiente o saldado.

## What Changes
- **Corrección de Código de Invitación**:
  - Ajuste de longitud a 8 caracteres alfanuméricos en la interfaz, etiquetas y validadores.
- **Gestión de Miembros y Reparto Variable**:
  - Lista de integrantes en la pestaña Hogar (nombre, email, rol y porcentaje).
  - Modal o panel para que el administrador asigne los porcentajes de división (`split_percentage`), validando que totalicen 100%.
  - Recálculo del balance de liquidación basado en los porcentajes configurados.
- **Eliminación y Abandono de Hogar**:
  - Opción de "Eliminar Hogar" para el creador/administrador (con diálogo de confirmación destructiva).
  - Opción de "Abandonar Hogar" para integrantes regulares.
- **Métricas del Mes en el Dashboard**:
  - Cálculo y visualización del gasto total del mes actual en la tarjeta principal del Dashboard.
- **Búsqueda y Filtros Avanzados en Historial de Gastos**:
  - Campo de búsqueda en vivo por nombre/descripción del gasto.
  - Filtro por mes (selector de período).
  - Filtro por categoría.
  - Filtro por rango de fechas (fecha inicial y fecha final).
  - Filtro por valor (monto exacto o rango mínimo - máximo).
  - Botón rápido para restablecer filtros.
- **Rediseño del Navigation Bar**:
  - Botón central `+` integrado directamente en la barra de navegación de 5 posiciones: `Inicio` | `Gastos` | `(+)` | `Flujo` | `Hogar`.
- **Ciclo de Facturación y Liquidación de Gastos del Hogar**:
  - Configuración de día de corte del ciclo (ej. 29 de cada mes).
  - Filtro de gastos del hogar pertenecientes al ciclo activo.
  - Historial de ciclos pasados con estado de liquidación: **Pendiente** (`PENDING`) o **Saldado** (`SETTLED`).

## Capabilities

### `household-management`
- Modelos y endpoints para ciclos de facturación (`billing_cycle_day`).
- Actualización de porcentajes variables de miembros (`PUT /api/v1/households/members/splits`).
- Eliminación de hogar (`DELETE /api/v1/households`) y abandono (`POST /api/v1/households/leave`).
- Historial de estados de ciclo (`GET /api/v1/households/cycles`).

### `android-client`
- Campo y validación para códigos de invitación de 8 caracteres.
- Pantalla y diálogo de miembros con ajuste de porcentajes.
- Acciones de eliminar hogar y abandonar hogar con confirmación.
- Tarjeta de Dashboard con gasto mensual acumulado.
- Barra de búsqueda y modal/filtro multifacético en `ExpensesListScreen`.
- Navbar con botón `+` integrado en el layout.
- Configuración de ciclo y visualización de historial de ciclos con badges Pendiente/Saldado.

## Impact
- Habilita la colaboración entre 2, 3 o más convivientes con cualquier distribución económica.
- Se adapta a las fechas de nómina y corte de servicios reales.
- Brinda herramientas de control y auditoría de gastos mucho más potentes al usuario.
