# Delta Spec: Gastos - Formulario, Fecha Personalizada y Filtros Avanzados

## 1. Requisitos Modificados

### REQ-EXP-01: Registro de Gastos con Fecha Personalizada
- **Comportamiento Anterior**: El gasto registrado recibía automáticamente la fecha del día actual del sistema (`today`).
- **Nuevo Comportamiento**:
  - Por defecto, la fecha del gasto continúa siendo la fecha actual (`today`).
  - La pantalla de registro debe presentar una opción/control ("¿Fecha diferente a hoy?") mediante Checkbox o Switch.
  - Al activarse, se despliega un campo de texto para la fecha con máscara o formato `YYYY-MM-DD` prellenado con la fecha de hoy, permitiendo al usuario ingresar cualquier día del mes actual o anterior.
  - Si el campo tiene un formato válido, se utiliza dicha fecha al crear el `ExpenseEntity` y al enviar la sincronización al servidor.

### REQ-EXP-02: Selector de Ámbito con Nombre Dinámico de Hogar
- **Comportamiento Anterior**: El botón de ámbito del hogar tenía texto fijo `"Hogar (50/50)"`.
- **Nuevo Comportamiento**:
  - El botón de ámbito del hogar debe obtener reactivamente el nombre del hogar (`householdName`).
  - Si el usuario pertenece a un hogar con nombre (ej. `"Casa"` o `"Apartamento 402"`), el texto del botón debe ser el nombre del hogar. Si está vacío, debe mostrar `"Hogar Compartido"`.

### REQ-EXP-03: Hoja de Filtros Avanzados Totalmente Desplegada
- **Comportamiento Anterior**: `ModalBottomSheet` en `ExpensesListScreen` iniciaba en estado `PartiallyExpanded`, cortando la mitad de las opciones de filtro.
- **Nuevo Comportamiento**:
  - `ModalBottomSheet` debe configurarse con `rememberModalBottomSheetState(skipPartiallyExpanded = true)`.
  - Debe abrirse completamente desplegado desde el primer toque, permitiendo al usuario ver el filtro de mes, chips de categorías, rango de fechas, montos y botones sin requerir gestos adicionales.
  - El contenido interno debe ser deslizable con scroll vertical si la pantalla del dispositivo tiene menor resolución.
