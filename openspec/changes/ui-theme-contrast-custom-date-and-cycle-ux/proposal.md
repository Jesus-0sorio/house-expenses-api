# Proposal: Corrección de Temas y Contraste, Fecha Personalizada de Gastos, Nombre Dinámico de Hogar, UX de Ciclos/Filtros y Datos de Prueba

## 1. Contexto y Justificación
Durante las pruebas de usuario en la aplicación móvil Android de **House Expenses**, se identificaron problemas críticos de usabilidad, legibilidad visual en modo claro, limitaciones en el registro de gastos históricos, textos estáticos confusos y falta de datos de prueba para los usuarios configurados:

1. **Contraste y Estilos Rotos en Fondos Claros (`AddExpenseScreen` y `EditExpenseDialog`)**:
   - Varios componentes (`AddExpenseScreen`, `CategoryChipSelector`, `EditExpenseDialog`) contienen colores fijos quemados (`Color.White`, `Color(0xFF1E293B)`).
   - Cuando la aplicación se usa con fondo blanco (tema claro), las etiquetas y textos se vuelven invisibles (blanco sobre blanco) y los campos de entrada se muestran como bloques oscuros con texto de difícil lectura.
   - El formulario de edición de gastos (`EditExpenseDialog`) presenta inconsistencias de contraste entre botones, fondos y chips.

2. **Imposibilidad de Registrar Gastos de Días Anteriores**:
   - Actualmente, al crear un nuevo gasto se asigna obligatoriamente la fecha actual (`Date()`). No existe una opción intuitiva para registrar compras pasadas (ej. factura de ayer o del fin de semana anterior).
   - El usuario requiere un selector o checkbox ("¿Fecha diferente a hoy?") que al activarse despliegue un campo editable para ingresar la fecha del gasto (`YYYY-MM-DD`).

3. **Texto Estático "Hogar 50/50"**:
   - El selector de ámbito muestra el botón estático `"Hogar (50/50)"`. Esto confunde a los usuarios cuando el hogar tiene repartos asimétricos (ej. 60/40) o más de dos integrantes, o simplemente porque no refleja la identidad del hogar.
   - Debe mostrarse el nombre dinámico del hogar (ej. `"Casa"`, `"Apartamento 402"`).

4. **Visualización de Ciclos en el Historial**:
   - En `HouseholdScreen`, las tarjetas muestran títulos técnicos como `"Ciclo #0: 2026-08-01 al 2026-08-31"`. El identificador `"Ciclo #0"` resulta confuso y anti-intuitivo para los usuarios.
   - Debe mostrarse únicamente el rango de fechas formateado de forma clara junto con el monto total y balance.

5. **ModalBottomSheet de Filtros Avanzados Cortado**:
   - En `ExpensesListScreen`, al pulsar en filtros avanzados, el `ModalBottomSheet` se abre por defecto en estado parcialmente expandido (`PartiallyExpanded`), obligando al usuario a deslizar hacia arriba manualmente para ver los selectores y el botón de aplicar.
   - Debe abrirse completamente expandido (`skipPartiallyExpanded = true`) y con scroll vertical si es necesario.

6. **Generación de Datos de Prueba para Jesús y Roomie**:
   - En el entorno de base de datos actual (PostgreSQL en Docker), los usuarios `demo@houseexpenses.com` (Jesus Osorio) y `roomie@houseexpenses.com` (Roomie Demo) están registrados en el hogar `"Casa"` pero tienen 0 gastos y 0 pagos.
   - Se requiere un script / seeder que inserte transacciones de prueba realistas (compras de mercado, servicios públicos, salidas de ocio y transferencias de liquidación) para validar de inmediato la visualización de balances, visibilidad cruzada y cálculo de deudas.

---

## 2. Alcance de la Propuesta

### A. Corrección de Temas y Estilos Dinámicos (Light/Dark Mode)
- Refactorizar `AddExpenseScreen.kt`, `CategoryChipSelector.kt` y `EditExpenseDialog.kt` para eliminar `Color.White`, `Color(0xFF1E293B)` y `Color(0xFF334155)` hardcodeados.
- Emplear la paleta semántica de `MaterialTheme.colorScheme`:
  - Contenedores: `MaterialTheme.colorScheme.surface` / `surfaceVariant`
  - Textos y etiquetas: `MaterialTheme.colorScheme.onSurface` / `onSurfaceVariant`
  - Bordes: `MaterialTheme.colorScheme.outline`
  - Acentos de marca: `EmeraldPrimary` y `PurpleAccent` con contraste garantizado en temas claro y oscuro.

### B. Fecha Personalizada en `AddExpenseScreen`
- Agregar un interruptor/checkbox: *"Gasto de fecha anterior o diferente a hoy"*.
- Estado booleano `isCustomDateEnabled` (por defecto `false`).
- Si está activo, mostrar un `OutlinedTextField` de fecha con validación `YYYY-MM-DD` inicializado con la fecha de hoy, permitiendo modificar el día/mes/año del gasto.
- Si no está activo, utilizar automáticamente la fecha actual (`today`).

### C. Nombre Dinámico del Hogar en Selector de Ámbito
- En `AddExpenseScreen` y `EditExpenseDialog`, vincular el nombre del hogar a `viewModel.householdName`.
- Sustituir `"Hogar (50/50)"` por `if (householdName.isNotBlank()) householdName else "Hogar Compartido"`.

### D. Historial de Ciclos Limpio
- En `HouseholdScreen.kt` y `CycleDetailBottomSheet`, eliminar el prefijo `"Ciclo #$cycleIndex"`.
- Mostrar limpiamente el rango de fechas (ej. `"2026-08-01 al 2026-08-31"`) y los datos financieros asociados.

### E. Expansión Inmediata de Filtros Avanzados
- En `ExpensesListScreen.kt`, configurar el `ModalBottomSheet` con:
  ```kotlin
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  ```
  y habilitar scroll vertical en su contenedor interno para garantizar acceso directo a todos los filtros sin arrastrar.

### F. Población de Datos de Prueba (Seed Data)
- Crear una herramienta de seeder (script SQL ejecutable vía Docker / script Go en `cmd/seed`) que registre para Jesús (`demo@houseexpenses.com`) y Roomie (`roomie@houseexpenses.com`):
  - Gasto de supermercado (Hogar): $280.000 COP pagado por Jesús.
  - Factura de servicios de luz y gas (Hogar): $190.000 COP pagado por Roomie.
  - Gasto de internet (Hogar): $95.000 COP pagado por Jesús.
  - Pago de liquidación parcial: $40.000 COP transferido de Roomie a Jesús.
  - Gastos personales para cada uno (demostrando aislamiento de privacidad).

---

## 3. Criterios de Aceptación

1. **Modo Claro / Oscuro**:
   - En `AddExpenseScreen` y `EditExpenseDialog`, todos los textos, inputs, selectores de chips y botones son 100% legibles sobre fondo blanco y sobre fondo oscuro.
2. **Fecha Personalizada**:
   - El usuario puede marcar la opción de fecha diferente y digitar una fecha específica (ej. `2026-08-28`). El gasto guardado persiste con esa fecha exacta.
3. **Identidad del Hogar**:
   - El botón de ámbito muestra `"Casa"` (o el nombre configurado por el usuario) en lugar del texto `"Hogar (50/50)"`.
4. **Historial de Ciclos**:
   - El historial de ciclos no incluye `"Ciclo #0"`, solo el rango de fechas y métricas.
5. **Filtros Avanzados**:
   - Al pulsar "Filtros", la hoja inferior se abre inmediatamente completa mostrando todos los controles (mes, categoría, rango de fechas, montos y botones).
6. **Datos de Prueba**:
   - Al iniciar sesión con Jesús y con el Roomie, el dashboard y la pantalla de hogar muestran de inmediato gastos compartidos, quién le debe a quién y el historial de pagos.
