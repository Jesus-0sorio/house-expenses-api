# Spec Delta: android-client

## ADDED REQUIREMENTS

### Requirement: Modal de Pago con Selección de Integrante Destinatario
Al abrir `SettlementPaymentDialog` en `HouseholdScreen.kt`:
- El modal SHALL mostrar un selector con los demás integrantes del hogar:
  - Etiqueta: *"¿A qué integrante le vas a pagar?"*.
  - Opciones: Lista de integrantes activos excluyendo al usuario autenticado.
  - Preselección automática: El integrante acreedor con el mayor saldo a favor pendiente de recibir.
- El modal SHALL ofrecer exclusivamente los canales:
  - `"Transferencia"`
  - `"Efectivo"`
- El modal SHALL incluir un campo numérico para el monto a saldar en COP, prellenado con el saldo sugerido.

### Requirement: Registro Visual de Pagos y Liquidaciones en Pantalla de Hogar
`HouseholdScreen.kt` SHALL mostrar una sección destacada de **"Registro de Pagos"** (o "Historial de Liquidaciones"):
- Mostrará cada transferencia del ciclo con:
  - Nombre del integrante que pagó y nombre del integrante que recibió (*"[Pagador] pagó a [Receptor]"*).
  - Monto formateado en COP.
  - Canal utilizado (Transferencia / Efectivo) y fecha.
- Si no se han registrado pagos en el ciclo, mostrar un mensaje informativo sutil (*"No se han registrado pagos en este ciclo."*).

### Requirement: Corrección de Consulta Outbox en `ExpenseDao`
`ExpenseDao.kt` SHALL consultar todos los registros no sincronizados:
- `@Query("SELECT * FROM expenses WHERE syncStatus != 'SYNCED'")`
- Esto garantiza que tanto los registros con estado `"PENDING"` como `"PENDING_SYNC"` sean procesados por `performSync()`.

### Requirement: Rehidratación de Sesión del Administrador e Integrante
Al iniciar sesión (`login`) o entrar a `HouseholdScreen`:
- El cliente SHALL consultar `GET /api/v1/households/current`.
- Si el usuario tiene un hogar activo, `SessionManager` y `ExpenseViewModel` SHALL rehidratar:
  - `activeHouseholdId`
  - `householdName`
  - `householdInviteCode`
  - `householdBillingCycleDay`
  - `isHouseholdAdmin`
- Se forzará una sincronización bidireccional (`performSync()`) para descargar los gastos del hogar de todos los integrantes y el registro de pagos antes de calcular balances locales.
