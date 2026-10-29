# Delta Spec: Expenses (Sincronización de Fechas Resiliente)

## ADDED Requirements

### Requirement: Deserialización Tolerante de Fechas en Sincronización Batch
El endpoint `POST /api/v1/sync` SHALL admitir campos de fecha tanto en formato de fecha corta ISO `"yyyy-MM-dd"` (`"2006-01-02"`) como en formato estándar ISO-8601 / RFC3339 (`"2006-01-02T15:04:05Z"`), así como valores nulos o vacíos.

#### Scenario: Cliente Android envía fecha plana en formato yyyy-MM-dd
- **GIVEN** un lote de sincronización con un gasto que contiene `"date": "2026-09-05"`
- **WHEN** el cliente envía la petición `POST /api/v1/sync`
- **THEN** el middleware y unmarshaler de Gin SHALL procesar la fecha exitosamente sin error HTTP 400
- **AND** el gasto SHALL ser persistido en la base de datos con la fecha correspondiente al inicio del día en UTC.

#### Scenario: Sincronización con fecha en formato RFC3339
- **GIVEN** un lote de sincronización con `"date": "2026-09-05T14:30:00Z"`
- **WHEN** el backend deserializa el payload
- **THEN** SHALL parsear correctamente la fecha y hora sin fallos de validación.

### Requirement: Persistencia y Vinculación Automática al Ciclo del Hogar
Cada gasto con `scope == "HOUSEHOLD"` sincronizado con éxito SHALL quedar vinculado al `ActiveHouseholdID` del usuario autenticado, asegurando que `GET /api/v1/household/balance` lo contabilice inmediatamente en el ciclo actual.

#### Scenario: Cálculo de balance del ciclo tras sincronizar gasto
- **GIVEN** un gasto de hogar sincronizado por $100.000 COP en la fecha actual
- **WHEN** se consulta `GET /api/v1/household/balance`
- **THEN** el campo `total_household` SHALL reflejar al menos $100.000 COP
- **AND** los balances individuales de los integrantes SHALL distribuir equitativamente el aporte y las cuotas esperadas.
