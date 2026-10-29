# Delta Spec: Android Client (Pantallas de Inicio, Historial, Flujo de Caja y Hogar)

## 1. DashboardScreen
- **Tarjeta de Gastos del Mes**:
  - Debe presentar el total consolidado del mes.
  - Debe incluir dos apartados destacados para desglose:
    - **Gastos Personales**: suma de gastos del mes con `scope == "PERSONAL"`.
    - **Gastos del Hogar**: suma de gastos del mes con `scope == "HOUSEHOLD"`.
  - Debe eliminarse el texto estático o de estado `"✓ Todo al día"`. Si existen elementos pendientes de sincronización (`pendingSync > 0`), se mantendrá únicamente el indicador de sincronización pendiente.
- **Card de Balance del Hogar**:
  - El texto de liquidación debe formatear las cifras monetarias aplicando el formato oficial de moneda colombiana sin decimales (`formatCOP.format(monto)` -> `$ 100.000`).
- **Transacciones del Mes**:
  - El encabezado de la sección debe titularse `"Transacciones del Mes"` en lugar de `"Transacciones Recientes"`.
  - La lista debe contener exclusivamente transacciones correspondientes al usuario conectado (`userId == currentUserId`).
  - La lista debe restringirse a transacciones cuya fecha comience con el mes actual (`currentMonthKey`).
  - La fecha en la tarjeta de transacción debe mostrarse limpia en formato `YYYY-MM-DD`.
- **Margen Inferior**:
  - Reducir el espaciador final de la lista a 16-24 dp.

## 2. ExpensesListScreen
- **Agrupación Jerárquica de Lista**:
  - La agrupación por días debe realizarse sobre la subcadena de fecha de 10 caracteres (`it.date.take(10)`).
  - Un único encabezado de día debe crearse por cada fecha calendario diferente, evitando repetición de encabezados por cada compra.
- **Modal de Filtros Avanzados**:
  - Se debe suprimir el campo de entrada "O Valor Específico" y su variable de estado asociada `exactAmountFilter`.
- **Margen Inferior**:
  - Reducir el espaciador final a 16-24 dp.

## 3. CashFlowScreen
- **Gastos del Día Realizado**:
  - En la sección de desglose diario, mostrar la lista de transacciones realizadas en la fecha actual (`todayDate`).
  - Cada tarjeta debe visualizar:
    - Nombre / descripción de la compra (`description`).
    - Fecha normalizada `YYYY-MM-DD`.
    - Monto formateado con `formatCOP`.
- **Compromisos Recurrentes y Suscripciones**:
  - Presentar una tarjeta/sección dedicada a *"Compromisos Recurrentes y Suscripciones"* con la lista de compromisos fijos mensuales (arriendo, streaming, servicios).
  - Cada elemento debe mostrar nombre, monto mensual, día de cobro ("Día X de cada mes") y etiqueta de ámbito (Personal / Hogar).
  - Incluir botón para abrir diálogo de agregar compromiso recurrente (`AddRecurringDebtDialog`).
- **Margen Inferior**:
  - Reducir el espaciador final a 16-24 dp.

## 4. HouseholdScreen
- **Protección de Edición de Gastos**:
  - La tarjeta de un gasto compartido solo debe responder al clic (`onClick`) si `exp.userId == currentUserId`.
  - Si el gasto fue registrado por otro integrante, debe mantenerse inactivo para edición y evidenciar quién lo pagó.
- **Margen Inferior**:
  - Reducir el espaciador final a 16-24 dp.

## 5. MainContainerScreen y AddExpenseScreen
- **Texto del Botón de Creación**:
  - En `MainContainerScreen.kt`, reemplazar el texto de la opción flotante `"Nuevo Gasto Manual"` por `"Nuevo Gasto"`.
- **Soporte de Recurrencia en AddExpenseScreen**:
  - Añadir checkbox *"¿Es un gasto recurrente mensual? (Arriendo, suscripciones, cuota)"*.
  - Al seleccionarse, mostrar un campo de texto numérico para el día de corte/cobro (1 a 31).
  - Al guardar, crear el gasto para el ciclo actual y registrar el pasivo recurrente en `createRecurringDebt`.
