# Delta Spec: Gestión del Hogar y Agregación de Gastos Generales

## ADDED Requirements

### Requirement: Agregación Unificada de Gastos del Hogar por Ciclo
El caso de uso del hogar SHALL proveer el cálculo del total general de gastos compartidos generados por todos los miembros del hogar durante el período del ciclo activo.

#### Scenario: Consulta del total acumulado por todos los miembros
- **GIVEN** un hogar activo con 2 o más miembros
- **WHEN** se invoca el cálculo de balance del hogar
- **THEN** el campo `total_household` SHALL ser exactamente la suma de todos los gastos con `scope = 'HOUSEHOLD'` de todos los integrantes en el ciclo.

### Requirement: Mapeo de Integrantes a Transacciones
El sistema SHALL permitir al cliente asociar de forma unívoca cada gasto compartido con el nombre del integrante que lo generó.

#### Scenario: Visualización del nombre del compañero en la transacción
- **GIVEN** que un compañero de hogar registró un gasto
- **WHEN** se consulta el listado de gastos del hogar
- **THEN** el registro SHALL exponer los datos del usuario pagador (`user_id`, `name`) para permitir su renderizado en la interfaz.
