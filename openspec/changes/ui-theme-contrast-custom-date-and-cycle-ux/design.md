# Design: Temas, Contraste de Formularios, Fecha de Gastos, UX de Ciclos y Datos de Prueba

## 1. Arquitectura y Enfoque de Diseño

### A. Paleta Semántica y Contraste en Compose (`Theme-Aware Styling`)
- **Problema actual**: El código contenía colores estáticos oscuros (`Color(0xFF1E293B)`, `Color(0xFF334155)`) y fuentes forzadas a `Color.White`. Cuando el usuario activa o se encuentra en modo claro (fondo blanco de `Scaffold`), el texto blanco de las etiquetas resulta ilegible y los campos de entrada aparecen como rectángulos oscuros sin armonía visual.
- **Solución**:
  - Reemplazar todas las referencias duras por propiedades del `MaterialTheme.colorScheme`:
    - `MaterialTheme.colorScheme.onSurface`: Color principal para texto de etiquetas y títulos en formularios (negro/gris oscuro en modo claro, blanco/gris claro en modo oscuro).
    - `MaterialTheme.colorScheme.surface`: Color de fondo de los `OutlinedTextField`.
    - `MaterialTheme.colorScheme.outline`: Color de borde no enfocado.
    - `EmeraldPrimary`: Color de acento para foco y botones de acción principal.
    - `MaterialTheme.colorScheme.surfaceVariant`: Fondo sutil para chips deseleccionados.
    - `MaterialTheme.colorScheme.onSurfaceVariant`: Texto de chips deseleccionados y placeholders.

### B. Selector de Fecha en `AddExpenseScreen`
- **Flujo de interacción**:
  1. Se añade una fila interactiva:
     ```kotlin
     Row(verticalAlignment = Alignment.CenterVertically) {
         Checkbox(
             checked = isCustomDateEnabled,
             onCheckedChange = { isCustomDateEnabled = it },
             colors = CheckboxDefaults.colors(checkedColor = EmeraldPrimary)
         )
         Text(
             text = "¿Gasto con fecha anterior o diferente a hoy?",
             fontSize = 13.sp,
             color = MaterialTheme.colorScheme.onSurface
         )
     }
     ```
  2. Si `isCustomDateEnabled == true`, se muestra un `OutlinedTextField` de fecha:
     - Etiqueta: "Fecha del Gasto (YYYY-MM-DD)"
     - Placeholder: "Ej. 2026-08-28"
     - Valor inicial: `currentDate`
  3. Al presionar "Guardar Gasto", la fecha asignada es:
     ```kotlin
     val expenseDate = if (isCustomDateEnabled && customDate.isNotBlank()) customDate.trim() else currentDate
     ```

### C. Nombre Dinámico del Hogar en Selector de Ámbito
- En `AddExpenseScreen` y `EditExpenseDialog`, el nombre del hogar proviene de `viewModel.householdName`:
  ```kotlin
  val householdLabel = if (householdName.isNotBlank()) householdName else "Hogar"
  ```
- En lugar de `"Hogar (50/50)"`, el botón muestra:
  ```kotlin
  Text(householdLabel, fontWeight = if (scope == "HOUSEHOLD") FontWeight.Bold else FontWeight.Normal)
  ```

### D. Historial de Ciclos Limpio en `HouseholdScreen`
- En `HouseholdScreen.kt`:
  - Cabecera de la tarjeta:
    ```kotlin
    Text(
        text = "${c.start_date} al ${c.end_date}",
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurface
    )
    ```
  - En `CycleDetailBottomSheet`:
    ```kotlin
    Text(
        text = if (detail != null) "${detail.start_date} al ${detail.end_date}" else "Detalle del Ciclo",
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        color = MaterialTheme.colorScheme.onSurface
    )
    ```

### E. Expansión Total de Filtros Avanzados en `ExpensesListScreen`
- En `ExpensesListScreen.kt`:
  ```kotlin
  val filterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  ModalBottomSheet(
      onDismissRequest = { showAdvancedFiltersSheet = false },
      sheetState = filterSheetState,
      containerColor = MaterialTheme.colorScheme.surface
  ) {
      Column(
          modifier = Modifier
              .fillMaxWidth()
              .verticalScroll(rememberScrollState())
              .padding(horizontal = 20.dp)
              .padding(bottom = 32.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
          // Controles de filtros completos
      }
  }
  ```

### F. Seeder de Datos de Prueba en PostgreSQL
- Script SQL para poblar la base de datos PostgreSQL (`house-expenses-postgres-1`):
  1. Insertar gastos compartidos (`scope = 'HOUSEHOLD'`) con `household_id = '260f13e0-3471-461c-b523-32d6e9d2e6a2'`:
     - Compra de supermercado (Éxito): $280.000 COP pagado por Jesús (`user_id = '40fc16c3-1560-49fb-b773-88acb2d89cb5'`).
     - Factura de luz y gas (EPM): $190.000 COP pagado por Roomie (`user_id = 'c8c5b5a2-030a-48bd-83ab-ade94f1a48c3'`).
     - Factura de internet fibra (Claro): $95.000 COP pagado por Jesús.
     - Compra de aseo y hogar (D1): $65.000 COP pagado por Roomie.
  2. Insertar pago de liquidación (`settlement_payments`):
     - Transferencia Bancolombia: $40.000 COP de Roomie a Jesús para abonar a la diferencia de compras.
  3. Insertar gastos personales (`scope = 'PERSONAL'`):
     - Salida de cine personal para Jesús: $35.000 COP (`household_id = null`).
     - Almuerzo de trabajo personal para Roomie: $22.000 COP (`household_id = null`).
