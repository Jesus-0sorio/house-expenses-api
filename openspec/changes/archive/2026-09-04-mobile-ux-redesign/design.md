# Design: Arquitectura de Componentes y Flujos Móviles

## 1. Arquitectura de Navegación
Implementación de un Scaffold central con NavigationBar (Material 3) gestionado por MainNavigationContainer.kt:
- **Item 1 - Inicio**: Resumen general, tarjeta de disponible quincenal, tarjeta de saldo 50/50 y últimas transacciones.
- **Item 2 - Gastos**: Lista filtrable por ámbito (Todos, Personal, Hogar) con pull-to-refresh.
- **Item 3 - Flujo de Caja**: Configuración de ingresos (quincenales, mensuales, extraordinarios), balance del ciclo y desglose de gasto diario efectuado.
- **Item 4 - Hogar**: Estado del balance 50/50 entre roomies y acción para registrar pago/transferencia compensatoria.
- **Floating Action Button**: Modal rápido para seleccionar Escanear Factura QR o Registrar Gasto manual.

## 2. Componente de Input de Moneda COP (CopCurrencyVisualTransformation)
Transformación visual en Compose que recibe dígitos numéricos y proyecta automáticamente el formato monetario con puntos de miles (1000000 -> $ 1.000.000).

## 3. Selector de Categorías en Chips (CategoryChipSelector)
Reemplazo del menú desplegable por una fila deslizable o cuadrícula de FilterChip estilizados con colores esmeralda/índigo e íconos temáticos para las categorías oficiales colombianas (Supermercado, Restaurantes, Servicios, Transporte, Salud, etc.).

## 4. Persistencia Online-First & Outbox Pattern
El repositorio ExpenseRepository.kt:
1. Ejecuta primero pi.createExpense(...).
2. Si la petición es exitosa, emite la lista remota.
3. Si ocurre IOException o fallo de red, almacena en AppDatabase con PENDING_SYNC para que SyncWorker lo procese automáticamente en segundo plano.
