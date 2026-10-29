# Delta Spec: Expenses (Historial, Agrupación Diaria, Filtros y Flujo de Caja)

## 1. Requisitos de Agrupación de Gastos
- **Normalización de Fecha por Día**:
  - Al agrupar transacciones en el historial (`ExpensesListScreen`), la clave de agrupación diaria debe considerar exclusivamente la porción de fecha en formato `YYYY-MM-DD` (`date.take(10)`).
  - Todos los gastos que coincidan en el mismo día del calendario deben consolidarse en una única sección `DayExpensesGroup`.
  - Debe renderizarse un único encabezado de día (`formatDayHeader`) que muestre la fecha y el total acumulado de ese día, seguido inmediatamente por todas las transacciones individuales ocurridas en esa jornada.

## 2. Requisitos de Filtros Avanzados
- **Eliminación de Filtro por Valor Específico**:
  - El sistema no debe requerir ni ofrecer un campo de texto para coincidencia exacta de monto ("O Valor Específico").
  - El filtrado de transacciones por monto debe basarse en límites flexibles de rango: valor mínimo (`minAmountFilter`) y valor máximo (`maxAmountFilter`).

## 3. Requisitos de Flujo de Caja (Gastos del Día)
- **Detalle de Gastos del Día Actual**:
  - La pantalla de flujo de caja debe listar las transacciones individuales realizadas en la fecha actual (`todayDate == exp.date.take(10)`).
  - Cada transacción mostrada en el flujo de caja diario debe incluir:
    - Descripción o nombre de la compra (`exp.description`).
    - Categoría del gasto (`exp.category`).
    - Fecha normalizada en formato `YYYY-MM-DD`.
    - Monto formateado en moneda local (`$ COP`).
  - Si en la fecha actual no se han registrado gastos, el sistema debe presentar un estado vacío informativo.

## 4. Requisitos de Presentación de Fechas y Espaciado
- **Formato Estricto de Fecha**:
  - Las listas y tarjetas de transacciones en la interfaz de usuario deben truncar cualquier información horaria o de zona (`HH:mm:ss`, `+00`, `Z`) y presentar la fecha en formato estricto `YYYY-MM-DD`.
- **Ajuste de Margen de Desplazamiento**:
  - El padding o espaciador inferior en las listas desplazables debe ser de 16-24 dp para evitar áreas vacías desproporcionadas en la parte inferior de la pantalla.

## 5. Requisitos de Gastos Recurrentes (Suscripciones, Arriendo y Servicios Fijos)
- **Opción de Recurrencia en Formulario de Gasto**:
  - En el formulario de creación de gasto (`AddExpenseScreen`), el usuario puede marcar la casilla *"Gasto recurrente mensual (arriendo, suscripción, cuota)"*.
  - Al activarse, debe habilitarse un selector de día de cobro/corte mensual (`due_day` del 1 al 31).
  - Al guardar, el sistema registra el gasto correspondiente al mes actual y crea el compromiso recurrente (`POST /finance/debts`) asignando el ámbito seleccionado (`PERSONAL` o `HOUSEHOLD`).
- **Consulta y Gestión de Compromisos Recurrentes**:
  - El usuario debe tener la capacidad de consultar y registrar compromisos recurrentes desde la interfaz de flujo de caja (`CashFlowScreen`) para controlar sus pasivos fijos mensuales.
