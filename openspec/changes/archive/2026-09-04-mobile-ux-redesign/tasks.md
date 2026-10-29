## 1. TopBar & Visual Currency Formatter
- [x] 1.1 Limpiar TopBar en DashboardScreen.kt: dejar solo Hola, {nombre}, quitar subtítulos de offline y eliminar badge/contador del botón de refrescar.
- [x] 1.2 Implementar CopCurrencyVisualTransformation.kt para formatear en vivo el valor ingresado como $ 1.000.000 con separador de miles.

## 2. Rediseño del Formulario de Gastos & Categorías
- [x] 2.1 Crear CategoryChipSelector.kt con chips visuales e íconos temáticos para categorías de gastos en Colombia.
- [x] 2.2 Actualizar AddExpenseScreen.kt: integrar el formateador COP en vivo, el selector de chips de categorías, eliminar campos de recurrencia/ingresos y condicionar selector de ámbito Personal/Hogar a si el usuario tiene un hogar activo.

## 3. Navegación con BottomBar & Nuevas Pestañas
- [x] 3.1 Implementar CashFlowScreen.kt con cálculo de dinero disponible quincenal/mensual, desglose de gasto diario efectuado hoy/por día y modal para registrar ingresos (quincenal, mensual, extraordinario).
- [x] 3.2 Implementar HouseholdScreen.kt con estado del balance 50/50 y diálogo de  Registrar Pago / Transferencia (Nequi, Daviplata, Bancolombia, Efectivo).
- [x] 3.3 Implementar ExpensesListScreen.kt con historial completo de gastos y filtro por ámbito (Todos, Personal, Hogar).
- [x] 3.4 Implementar MainContainerScreen.kt con NavigationBar de 4 pestañas (Inicio, Gastos, Flujo, Hogar) y botón de acción rápida central (+ / QR).

## 4. Escáner QR DIAN Optimizado (Opción A) & Repositorio Online-First
- [x] 4.1 Actualizar QRScannerScreen.kt para abrir directamente una hoja inferior (ModalBottomSheet) con el monto detectado en COP, emisor, switch Personal/Hogar y botón de guardado en 2 taps.
- [x] 4.2 Ajustar ExpenseRepository.kt para operar bajo el patrón Online-First con outbox transparente (guardar en SQLite solo ante fallas de red sin alarmar al usuario con tecnicismos).
