# Proposal: Visibilidad Unificada de Gastos Generales del Hogar, Sincronización Bidireccional Cruzada y Totales Globales

## Why
A pesar de la implementación de la visibilidad en consultas de lectura en el backend, los usuarios siguen experimentando que los gastos del hogar no se muestran en conjunto ni son visibles para los demás integrantes. Tras una exhaustiva inspección técnica de los flujos de red y almacenamiento local, se identificaron los siguientes problemas críticos:

1. **Fallo en la Ingesta de Sincronización Batch (`POST /api/v1/sync`)**:
   - En el backend Go (`sync_usecase.go`), la estructura `SyncExpenseItem` exige `ClientID string json:"client_id" binding:"required"`.
   - La aplicación Android envía en su payload `ExpenseSyncDto` el identificador con la clave `id` (no `client_id`). Por este motivo, el middleware de Gin rechaza la sincronización con un error HTTP `400 Bad Request` por validación de campo requerida.
   - Debido a esto, los gastos registrados en un teléfono nunca llegan a persistirse en la base de datos Postgres del backend.
2. **Error de Deserialización de Respuesta en Android (`JsonSyntaxException`)**:
   - El backend Go responde al endpoint `/sync` con una lista de objetos `SyncItemAck` (`synced_expenses: [{"client_id": "...", "server_id": "...", "status": "SYNCED"}]`).
   - El DTO de Android `SyncResponse` esperaba una lista plana de cadenas (`val synced_expenses: List<String> = emptyList()`).
   - Al recibir objetos JSON donde esperaba strings, Gson arroja una excepción de sintaxis que hace fallar `createExpense` y `performSync`, provocando que el gasto quede permanentemente en estado local `PENDING_SYNC` y nunca se complete la descarga remota de transacciones de otros compañeros.
3. **Omisión de `householdId` en la creación local de gastos**:
   - En `ExpenseViewModel.kt` (`createExpense` y `updateExpense`), la entidad `ExpenseEntity` se creaba sin asignar el campo `householdId` (dejándolo en `null`), aun cuando el ámbito seleccionado era `HOUSEHOLD`.
4. **Falta de Sincronización Inmediata y Proactiva en Corrutinas**:
   - Android delegaba la sincronización al `WorkManager` del sistema operativo, el cual puede retrasarse indefinidamente por optimización de batería o políticas de red. La app debe sincronizar de forma inmediata y reactiva en primer plano cada vez que se crea un gasto o se abren las pantallas principales.
5. **Falta de Vista de "Gastos Generales del Hogar" y Total Acumulado Unificado**:
   - En la interfaz de usuario, no se visualiza el **Total General de Gastos del Hogar** en conjunto (todos los integrantes combinados).
   - En la lista de gastos del hogar, cada ítem no indica de forma visible quién registró o pagó el gasto (avatar/nombre del integrante pagador), lo que genera confusión respecto a si los gastos son propios o de los demás integrantes.

---

## What Changes

### Backend Go:
- **Compatibilidad Bidireccional en `SyncExpenseItem`**:
  - Permitir que el backend acepte indistintamente `client_id` o `id` como identificador de cliente, removiendo el `binding:"required"` estricto sobre `ClientID` para garantizar interoperabilidad con clientes móviles existentes.
  - Asegurar que si `household_id` no viene en el payload del item pero el usuario tiene un hogar activo (`user.ActiveHouseholdID != nil`), se asigne automáticamente a gastos de ámbito `HOUSEHOLD`.
- **Adaptabilidad en `SyncResponse`**:
  - Asegurar que la respuesta de sincronización sea compatible o admita mapeo directo en Android tanto para identificadores como para objetos de confirmación.

### Frontend Android:
- **Corrección de DTOs en `SyncBatchDto.kt`**:
  - Modificar `ExpenseSyncDto` para enviar tanto `id` como `client_id`, y `household_id` explícito.
  - Actualizar `SyncResponse` para soportar la deserialización adecuada de la respuesta del servidor (`SyncItemAckDto` con fallback o extracción de IDs) evitando excepciones de Gson.
- **Asignación Correcta de `householdId` en `ExpenseViewModel.kt`**:
  - Al registrar (`createExpense`) o editar (`updateExpense`) un gasto con `scope == "HOUSEHOLD"`, poblar `householdId` con `sessionManager.activeHouseholdId`.
- **Sincronización Inmediata y Reactiva**:
  - Modificar `createExpense`, `syncNow` y `loadHouseholdBalance` para invocar inmediatamente `repository.performSync()` en segundo plano mediante corrutinas, descargando de inmediato los gastos creados por otros miembros.
- **Rediseño de "Gastos Generales del Hogar" en `HouseholdScreen.kt`**:
  - Incorporar una tarjeta destacada de **"Total Gastos Generales"** que muestre la suma global de todas las compras compartidas de la casa en el ciclo actual.
  - En la lista de gastos del hogar, mostrar cada gasto con el nombre y avatar de **quién lo pagó/registró** (ejemplo: *"Pagado por: Jesus (Tú)"* o *"Pagado por: Maria (Roomie)"*).
- **Indicador de Pagador en `ExpensesListScreen.kt`**:
  - Al filtrar por "Hogar" o listar transacciones compartidas, mostrar claramente qué integrante realizó cada pago.
- **Claridad de Totales en `DashboardScreen.kt`**:
  - Separar visualmente el Total General de Gastos del Hogar del Total de Gastos Personales del usuario.

---

## Capabilities

### Modified Capabilities
- `expenses`: Protocolo de sincronización batch robusto que admite llaves `id`/`client_id` y deserialización a prueba de fallos, garantizando persistencia inmediata en Postgres.
- `android-client`: Interfaz unificada de gastos generales con identificación del pagador, visualización de total global y sincronización proactiva en corrutinas.
- `household-management`: Integración de totales agregados del hogar para el ciclo activo y sincronización cruzada sin fricciones entre roomies.

---

## Impact
- Los roomies podrán ver las compras realizadas por cualquiera de los integrantes en tiempo real al sincronizar.
- Se elimina el problema de datos atascados en `PENDING_SYNC` en el cliente móvil.
- El total acumulado de gastos generales de la casa será visible de inmediato tanto en la pantalla del Hogar como en el Dashboard.
