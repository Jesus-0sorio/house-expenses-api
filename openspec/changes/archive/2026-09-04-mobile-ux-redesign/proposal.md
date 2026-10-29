# Proposal: Rediseño Integral de UX/UI y Experiencia Móvil Android

## Why
La aplicación móvil Android requiere una experiencia de usuario ágil, moderna y sin fricción, eliminando tecnicismos de sincronización (como menciones de modo offline) y simplificando el flujo de registro diario. Además, se requiere una arquitectura de navegación fluida con barra inferior (Bottom Navigation Bar) para gestionar el flujo de caja quincenal/mensual, el gasto real diario y la liquidación de cuentas compartidas en el hogar (50/50).

## What Changes
1. **Estrategia Online-First con Cola Outbox**:
   - La app consume y muestra prioritariamente datos del backend.
   - La base de datos local SQLite (Room) actúa como cola outbox solo para registrar gastos cuando el backend esté inaccesible o sin red, sincronizándolos automáticamente en segundo plano.
2. **Navegación por Pestañas (Bottom Navigation Bar)**:
   - 4 secciones: Inicio (Dashboard), Gastos (Historial/Filtro), Flujo de Caja (Ingresos y Gasto Diario), Hogar (Balance 50/50 y Saldar Deudas).
   - Botón de Acción Rápida Central (+ / QR) para registro express.
3. **Optimización Visual del Registro de Gastos**:
   - Input de monto con formateo en vivo de moneda colombiana ($ 1.000.000 COP).
   - Selector de categorías mediante chips visuales con íconos temáticos.
   - Eliminación de campos de recurrencia e ingresos en el modal de gastos.
   - Triaje de ámbito (Personal vs Hogar) solo visible si el usuario pertenece a un hogar activo.
4. **Escaneo de Factura DIAN en 2 Taps**:
   - BottomSheet pre-llenado con total, comercio y fecha detectada por QR para guardar en dos toques.
5. **Flujo de Caja con Gasto Real Diario e Ingresos Configurables**:
   - Gestión de ingresos quincenales, mensuales y extraordinarios (primas legales, bonos, freelance).
   - Cálculo del disponible real para el ciclo y visualización del gasto diario efectuado hoy y por día.
6. **Liquidación y Registro de Pago en el Hogar**:
   - Botón directo para registrar transferencias (Nequi, Daviplata, Efectivo, Banco) y saldar balances.
