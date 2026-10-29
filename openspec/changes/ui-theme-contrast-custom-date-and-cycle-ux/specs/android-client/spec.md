# Delta Spec: Cliente Android - Corrección de Estilos, Temas y Seeding de Pruebas

## 1. Requisitos Modificados

### REQ-AND-THEME-01: Adaptabilidad de Formularios a Modo Claro y Oscuro
- **Comportamiento Anterior**: Los componentes `AddExpenseScreen`, `CategoryChipSelector` y `EditExpenseDialog` tenían colores fijos `Color.White` para textos y `Color(0xFF1E293B)` para fondos de inputs y chips, generando pérdida total de contraste al usar fondo blanco (tema claro).
- **Nuevo Comportamiento**:
  - `AddExpenseScreen`:
    - Las etiquetas de "Monto", "Descripción", "Tipo de Cuenta", "Categoría" y "Fecha" deben usar `MaterialTheme.colorScheme.onSurface`.
    - `OutlinedTextField`: Contenedores deben usar `MaterialTheme.colorScheme.surface`, bordes `MaterialTheme.colorScheme.outline`, texto `MaterialTheme.colorScheme.onSurface` y placeholders `MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)`.
  - `CategoryChipSelector`:
    - La etiqueta de "Categoría" debe usar `MaterialTheme.colorScheme.onSurface`.
    - Los `FilterChip` deseleccionados deben usar `containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)` y `labelColor = MaterialTheme.colorScheme.onSurfaceVariant`.
    - Los `FilterChip` seleccionados deben usar `selectedContainerColor = EmeraldPrimary` y `selectedLabelColor = Color.Black`.
  - `EditExpenseDialog`:
    - Todas las etiquetas y campos deben alinearse estrictamente al esquema del tema `surface` / `onSurface`.
    - El selector de ámbito debe mostrar el nombre del hogar en lugar de `"Hogar 50/50"`.

### REQ-AND-SEED-01: Datos de Prueba para Jesús y Roomie
- **Requisito**:
  - El sistema debe contar con un script de inserción (seed) para los usuarios registrados en el hogar `"Casa"`:
    - Jesús Osorio (`demo@houseexpenses.com`, ID: `40fc16c3-1560-49fb-b773-88acb2d89cb5`)
    - Roomie Demo (`roomie@houseexpenses.com`, ID: `c8c5b5a2-030a-48bd-83ab-ade94f1a48c3`)
    - Hogar `"Casa"` (ID: `260f13e0-3471-461c-b523-32d6e9d2e6a2`)
  - Transacciones a generar:
    - Gastos del Hogar compartidos con variedad de fechas del ciclo actual.
    - Gastos personales para demostrar que solo cada usuario ve sus propios gastos individuales.
    - Un pago de liquidación registrado entre ambos roomies.
