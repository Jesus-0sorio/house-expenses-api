# Delta Spec: Sincronización Inmune a Fallas y Visibilidad Global de Gastos

## ADDED Requirements

### Requirement: Sincronización Batch con Soporte Polimórfico de Identificadores
El endpoint `POST /api/v1/sync` SHALL aceptar elementos de gasto donde el identificador único del cliente provenga indistintamente del campo `client_id` o `id`.

#### Scenario: Cliente móvil sincroniza con campo id
- **GIVEN** un usuario autenticado perteneciente a un hogar activo
- **WHEN** envía un payload de sincronización en `POST /api/v1/sync` conteniendo un gasto con `"id": "f5a2...b4"`, `"amount": 85000`, `"scope": "HOUSEHOLD"`
- **THEN** el backend SHALL procesar el gasto sin rechazarlo por error de validación `ClientID required`
- **AND** SHALL persistir el gasto en la base de datos asociando su `household_id` al hogar activo del usuario.

#### Scenario: Asignación automática de household_id
- **GIVEN** un usuario con `ActiveHouseholdID` configurado
- **WHEN** el cliente sincroniza un gasto con `scope = "HOUSEHOLD"` y `household_id` ausente o nulo
- **THEN** el backend SHALL asignar automáticamente `household_id = user.ActiveHouseholdID`.

### Requirement: Confirmación Estructurada y Resiliente de Sincronización
La respuesta del endpoint `POST /api/v1/sync` SHALL contener la lista de gastos e ingresos sincronizados exitosamente, permitiendo a los clientes móviles deserializar el resultado sin excepciones de tipado.

#### Scenario: Deserialización de confirmaciones en Android
- **GIVEN** una sincronización por lotes exitosa en el backend
- **WHEN** el backend retorna `synced_expenses` con la lista de confirmaciones
- **THEN** el cliente móvil Android SHALL deserializar cada confirmación extrayendo el ID sincronizado
- **AND** SHALL actualizar localmente el estado del gasto de `PENDING_SYNC` a `SYNCED` en la base de datos Room.

## MODIFIED Requirements

### Requirement: Consulta de Gastos con Preload de Creador
El endpoint `GET /api/v1/expenses` SHALL retornar todos los gastos personales del usuario autenticado y todos los gastos compartidos del hogar activo (`scope = 'HOUSEHOLD' AND household_id = ?`), incluyendo los datos del usuario que registró el gasto (`user.name`, `user.id`).

#### Scenario: Miembro consulta gastos compartidos
- **GIVEN** que el Usuario A registró un gasto compartido del hogar
- **WHEN** el Usuario B (miembro del mismo hogar) consulta `GET /api/v1/expenses`
- **THEN** el listado SHALL incluir el gasto compartido registrado por el Usuario A
- **AND** el campo `user_id` SHALL corresponder al Usuario A
- **AND** el objeto SHALL incluir el nombre o referencia del pagador para su despliegue visual en la interfaz.
