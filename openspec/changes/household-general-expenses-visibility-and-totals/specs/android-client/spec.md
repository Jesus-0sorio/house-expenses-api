# Delta Spec: Cliente Android para Gastos Generales del Hogar y Sincronización Proactiva

## ADDED Requirements

### Requirement: Tarjeta y Sección de Total de Gastos Generales del Hogar
En `HouseholdScreen.kt`, la aplicación SHALL presentar una tarjeta destacada con el **Total de Gastos Generales del Hogar**, calculando la suma de todos los gastos compartidos (`scope == "HOUSEHOLD"`) del ciclo activo.

#### Scenario: Visualización del total general de la casa
- **GIVEN** un hogar compartido con gastos registrados por diferentes integrantes (ej. $230.000 de servicios y $555.000 de supermercado)
- **WHEN** el usuario navega a la pantalla de Hogar
- **THEN** la interfaz SHALL exhibir de forma prominente el Total General del Hogar ($785.000 COP)
- **AND** SHALL listar todos los gastos generales en conjunto.

### Requirement: Identificación del Integrante Pagador en cada Gasto
Cada tarjeta de gasto con ámbito `HOUSEHOLD` mostrada en `HouseholdScreen.kt` y `ExpensesListScreen.kt` SHALL mostrar el nombre o distintivo del integrante que lo pagó o registró.

#### Scenario: Identificación de quién pagó un gasto
- **GIVEN** un gasto compartido registrado por otro integrante del hogar
- **WHEN** el usuario consulta la lista de gastos del hogar o el historial general
- **THEN** la tarjeta SHALL exhibir un distintivo que indique "Pagado por: [Nombre del Miembro]"
- **AND** si el gasto fue registrado por el usuario en sesión, SHALL indicar "Pagado por: Tú".

### Requirement: Sincronización Proactiva en Primer Plano
Al registrar o editar un gasto, o al abrir las pantallas principales (`DashboardScreen`, `HouseholdScreen`), el cliente SHALL invocar `performSync()` directamente en una corrutina de ViewModel sin depender de la latencia de WorkManager.

#### Scenario: Sincronización inmediata al guardar gasto
- **GIVEN** que el usuario presiona "Guardar Gasto" para una compra del hogar
- **WHEN** la inserción local en SQLite concluye
- **THEN** el ViewModel SHALL disparar una llamada inmediata a la API de sincronización
- **AND** SHALL actualizar el balance y la lista compartida de inmediato.

## MODIFIED Requirements

### Requirement: Serialización Robusta de Sincronización
La clase `ExpenseSyncDto` SHALL proveer tanto el campo `id` como `client_id`, y la clase `SyncResponse` SHALL admitir la confirmación estructurada del backend sin provocar `JsonSyntaxException`.

#### Scenario: Sincronización transparente de lotes
- **GIVEN** gastos locales con estado `PENDING`
- **WHEN** se ejecuta `performSync()`
- **THEN** el payload SHALL ser aceptado con HTTP 201 por el backend
- **AND** los gastos sincronizados SHALL pasar a estado `SYNCED` en SQLite.
