# Tasks: Corrección de Temas y Contraste, Fecha Personalizada de Gastos, Nombre Dinámico de Hogar, UX de Ciclos/Filtros y Datos de Prueba

## 1. Android UI: Corrección de Estilos y Contraste en Temas (Modo Claro / Oscuro)
- [x] 1.1 En `AddExpenseScreen.kt`, reemplazar todos los colores estáticos (`Color.White`, `Color(0xFF1E293B)`, `Color(0xFF334155)`) por propiedades semánticas de `MaterialTheme.colorScheme` (`onSurface`, `surface`, `outline`, `onSurfaceVariant`).
- [x] 1.2 En `CategoryChipSelector.kt`, actualizar el color de la etiqueta "Categoría" a `MaterialTheme.colorScheme.onSurface`, y definir los colores de `FilterChipDefaults.filterChipColors` adaptables a temas claro y oscuro.
- [x] 1.3 En `EditExpenseDialog.kt`, corregir contrastes de texto, fondos de `OutlinedTextField` y chips para que sean 100% legibles en modo claro y modo oscuro.

## 2. Experiencia de Registro: Fecha Personalizada y Nombre Dinámico del Hogar
- [x] 2.1 En `AddExpenseScreen.kt`, implementar la opción de fecha personalizada:
  - Añadir checkbox / control: *"¿Gasto con fecha anterior o diferente a hoy?"*.
  - Mostrar campo de texto `OutlinedTextField` para la fecha cuando la opción esté seleccionada (prellenado con la fecha de hoy).
  - Usar la fecha personalizada al guardar el gasto si la opción está activa.
- [x] 2.2 En `AddExpenseScreen.kt` y `EditExpenseDialog.kt`, reemplazar el texto estático `"Hogar (50/50)"` por el nombre real y dinámico del hogar (`viewModel.householdName`), usando `"Hogar Compartido"` como respaldo si no tiene nombre.

## 3. UX de Ciclos y Filtros Avanzados
- [x] 3.1 En `HouseholdScreen.kt`, limpiar la presentación del historial de ciclos:
  - Eliminar `"Ciclo #${c.cycle_index}: "` de los títulos de las tarjetas del historial.
  - Mostrar limpiamente el rango de fechas (`"${c.start_date} al ${c.end_date}"`).
  - Actualizar el título de `CycleDetailBottomSheet` para mostrar el rango de fechas en lugar de `"Detalle del Ciclo #..."`.
- [x] 3.2 En `ExpensesListScreen.kt`, actualizar `ModalBottomSheet` de filtros avanzados:
  - Configurar `sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)` para que se abra completamente expandido sin necesidad de arrastrar.
  - Añadir `verticalScroll` al contenedor interno para asegurar navegación fluida en cualquier resolución de pantalla.

## 4. Seeder de Datos de Prueba y Verificación
- [x] 4.1 Crear y ejecutar el script de seed en PostgreSQL para poblar datos de prueba en los usuarios de Jesús (`demo@houseexpenses.com`) y Roomie (`roomie@houseexpenses.com`):
  - Gastos compartidos del hogar en diferentes fechas y categorías.
  - Gastos personales para cada integrante.
  - Pago de liquidación entre roomies.
- [x] 4.2 Ejecutar `go test -v -count=1 ./...` y comprobar que todos los casos de uso y balance compensado continúen pasando con 0 errores.
- [x] 4.3 Validar que los endpoints del backend retornen las transacciones pobladas y que la aplicación móvil las sincronice correctamente.
