# Proposal: Visibilidad de Integrantes para Admin, Gastos Totales del Hogar y Registro de Pagos de Liquidación Entre Roomies

## Why
Tras las pruebas de uso con roles de administrador e integrantes en el hogar compartido, se detectaron fallas estructurales y funcionales en la gestión del hogar:

1. **El Administrador no visualiza a los integrantes ni los gastos totales del hogar**:
   - **Causa en Persistencia Local**: En `ExpenseDao.kt`, la consulta de sincronización pendiente buscaba exclusivamente `@Query("SELECT * FROM expenses WHERE syncStatus = 'PENDING'")`, mientras que el repositorio guardaba los gastos como `"PENDING_SYNC"`. Esto provocaba que cualquier gasto no enviado en tiempo real quedara varado en Room y nunca se reintentara en segundo plano.
   - **Causa en Sesión**: Al iniciar sesión mediante `POST /auth/login`, la app únicamente almacenaba `active_household_id`, pero no restauraba `household_name`, `invite_code`, `is_household_admin` ni el día de corte. Como consecuencia, si el admin cerraba sesión o iniciaba sesión en un nuevo dispositivo, la aplicación consideraba que `householdName` estaba vacío y los controles administrativos o la vista completa del hogar no se inicializaban correctamente.
   - **Causa en Descarga Cruzada**: Los gastos compartidos creados por otros integrantes no se estaban reflejando de forma reactiva en el dispositivo del administrador porque faltaba una sincronización completa (`pull`) de los gastos con `scope = "HOUSEHOLD"` al cargar la pantalla de hogar.

2. **Ausencia de una Entidad Real para "Pagos de Liquidación" (Saldar Cuentas)**:
   - Hasta el momento, cuando un integrante presionaba *"Registrar Pago / Transferencia"*, el sistema creaba un `Expense` ordinario de categoría `"Servicios"` y scope `"HOUSEHOLD"`.
   - **Efecto Negativo Crítico**: Al registrar un pago como gasto del hogar, el monto total de gastos de la casa se incrementaba artificialmente (por ejemplo, si Bob le transfería $100.000 a Alice para pagarle su deuda, el sistema registraba una nueva compra de $100.000 para la casa y volvía a dividir esa compra entre ambos).
   - **Requerimiento Real**: Un pago para saldar cuentas **no es una compra del hogar**; es una transferencia directa entre un integrante deudor (`payer_user_id`) y un integrante acreedor (`receiver_user_id`) dentro del hogar (`household_id`). Debe reducir la deuda del que paga y el saldo a favor del que recibe, sin inflar el total de gastos acumulados de la casa.

3. **Falta de Selección del Destinatario en el Modal de Pago**:
   - El diálogo de pago únicamente solicitaba el canal ("Transferencia" / "Efectivo") y el monto, sin permitir elegir a **cuál integrante del hogar se le está realizando el pago** para que las cuentas queden equilibradas.

4. **Falta de un "Registro de Pagos" Visible en la Pantalla de Hogar**:
   - Se requiere que tanto el admin como los demás integrantes puedan ver una sección dedicada con el historial de todos los pagos y transferencias realizadas entre los integrantes del hogar en el ciclo activo.

---

## What Changes

### 1. Backend Go: Entidad `SettlementPayment` y Balance de Cuentas Equilibrado
- **Nuevo Modelo de Dominio**: Crear la entidad `SettlementPayment` con:
  - `id`: UUID.
  - `household_id`: UUID (vinculación estricta con el hogar).
  - `payer_user_id`: UUID (quién realiza el pago).
  - `receiver_user_id`: UUID (a quién se le transfiere/paga).
  - `amount`: float64 (monto en COP).
  - `channel`: string ("Transferencia" o "Efectivo").
  - `date`: time.Time (fecha de pago).
  - `notes`: string opcional.
- **Endpoints REST**:
  - `POST /api/v1/households/payments`: Registra una transferencia de liquidación entre dos integrantes del hogar activo.
  - `GET /api/v1/households/payments`: Retorna la lista de pagos de liquidación del ciclo/hogar activo.
- **Sincronización Batch**: Integrar `payments` dentro del endpoint `POST /api/v1/sync` para permitir sincronización sin conexión de transferencias de liquidación.
- **Ajuste Matemático en `CalculateHouseholdBalance`**:
  - Los gastos del hogar (`scope = "HOUSEHOLD"`) definen el `total_household` y la cuota esperada de cada miembro (`expected_share = total_household * split_percentage / 100`).
  - Los pagos de liquidación ajustan el saldo neto:
    $$\text{net\_balance}_u = (\text{gastos\_pagados}_u - \text{cuota\_esperada}_u) + \text{pagos\_enviados}_u - \text{pagos\_recibidos}_u + \text{rollover\_anterior}_u$$
  - Esto garantiza que cuando Bob le paga su deuda a Alice, la deuda se reduce a $0 sin alterar los gastos reales de compras o servicios del hogar.
- **Endpoint de Información Completa del Hogar**:
  - Asegurar que `GET /api/v1/households/current` entregue nombre del hogar, código de invitación, rol del usuario (ADMIN o MEMBER), día de corte y lista de miembros, permitiendo la reconstrucción fidedigna del estado en el cliente al iniciar sesión o recargar.

### 2. Frontend Android: Gestión Local de Pagos y Resolución de Sincronización
- **Entidad Room `SettlementPaymentEntity` y DAO**:
  - Crear la tabla local `household_payments` con `id`, `householdId`, `payerUserId`, `payerName`, `receiverUserId`, `receiverName`, `amount`, `channel`, `date`, `syncStatus`.
- **Corrección en `ExpenseDao` y Sincronización**:
  - Homogeneizar los estados en `ExpenseDao.kt` (`WHERE syncStatus IN ('PENDING', 'PENDING_SYNC')` o `WHERE syncStatus != 'SYNCED'`) para garantizar que ningún gasto se quede sin sincronizar.
  - Al sincronizar (`performSync()`), descargar tanto los gastos del hogar como los pagos de liquidación del backend.
- **Recuperación Automática de Datos de Hogar**:
  - En `ExpenseViewModel.loadHouseholdBalance()` y `AuthViewModel.login()`, consultar la información completa del hogar del usuario en el servidor, actualizando reactivamente `householdName`, `householdInviteCode`, `isHouseholdAdmin` y `billingCycleDay`.

### 3. Frontend Android: UI de Hogar Renovada en `HouseholdScreen.kt`
- **Tarjeta de Balance del Ciclo Activo**:
  - Total consolidado de gastos del hogar.
  - Cuentas y aportes por integrante (pagado en compras, cuota debida, saldo neto resultante).
  - Caja de liquidación directa con instrucciones claras (*"Debes pagar $X a [Nombre]"*).
- **Selector de Destinatario en `SettlementPaymentDialog`**:
  - El modal de pago ahora incluye:
    - Selector interactivo del integrante destinatario (*"¿A quién le vas a pagar?"*), preseleccionando al roomie acreedor principal.
    - Opciones de canal restringidas a **"Transferencia"** y **"Efectivo"**.
    - Monto sugerido y campo editable.
- **Sección de "Registro de Pagos y Liquidaciones"**:
  - Lista visual cronológica de las transferencias registradas entre integrantes dentro del ciclo activo (*"[Nombre Pagador] pagó $X a [Nombre Receptor] vía [Canal]"*).
- **Lista Completa de Gastos del Hogar**:
  - Muestra todas las compras compartidas realizadas por todos los miembros del hogar, con indicación de quién realizó la compra.

---

## Capabilities

### Modified Capabilities
- `household-management`: Se expande para soportar transferencias de liquidación (`SettlementPayment`), balance neto compensado por pagos entre integrantes, y endpoint de consulta de estado del hogar.
- `expenses`: Se desacoplan los pagos de saldar cuentas del registro de compras del hogar para evitar la duplicación o inflación de gastos.
- `android-client`: Se incorporan la entidad y vista de pagos de liquidación, selector de integrante destinatario, corrección de sincronización outbox en Room y restauración integral de la sesión del admin.

---

## Impact
- **Base de Datos**: Nueva tabla `settlement_payments` con llaves foráneas a `households` y `users`.
- **Integridad de Datos**: Los gastos del hogar reflejan únicamente compras y servicios reales; las deudas se compensan con precisión mediante transferencias directas entre integrantes.
- **Experiencia de Usuario**: El administrador y todos los integrantes cuentan con una vista unificada, transparente y en tiempo real del ciclo activo, quién debe a quién y el registro de pagos efectuados.
