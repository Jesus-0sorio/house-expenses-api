# Proposal: Sincronización de Gastos del Hogar, Balance Detallado, Expulsión de Miembros, Rollover de Deudas y Scroll Infinito

## Why
Tras el despliegue inicial de ciclos y reparto variable, la experiencia de usuario y las pruebas en dispositivos reales revelaron los siguientes problemas críticos y necesidades operativas:

1. **El Balance del Hogar no muestra los totales ni las deudas individuales**:
   - En la aplicación Android no se visualiza el total acumulado de la casa, ni lo que ha pagado cada integrante, ni cuánto debe cada persona.
   - La causa raíz radica en una discrepancia entre el backend (que devuelve `SettlementSummary` con `total_household`, `balances` detallados con `user_id`, `user_name`, `total_paid`, `expected_share`, `net_balance` y `settlement`) y el DTO móvil `HouseholdBalanceResponse`, el cual esperaba campos obsoletos (`user_paid`, `roomie_paid`, `settlement_summary`). Al no coincidir las claves JSON, la app mostraba valores en cero y el texto por defecto.
2. **Los gastos compartidos no son visibles para otros integrantes de la casa**:
   - Si el Usuario A registra una compra de supermercado con ámbito `HOUSEHOLD`, e inicia sesión el Usuario B (otro miembro del mismo hogar), el gasto no aparece en la lista de transacciones de B ni en sus pantallas locales.
   - El endpoint backend `GET /api/v1/expenses` únicamente consulta `expenseRepo.FindByUser(userID)`, ignorando los gastos creados por otros miembros dentro del mismo `household_id` activo.
3. **El Administrador no puede expulsar o sacar integrantes**:
   - El administrador del hogar no tiene una opción ni endpoint para remover a un integrante (por ejemplo, si un roomie se muda o alguien se unió por error). Solo existía la opción de que el miembro abandonara voluntariamente o que el admin eliminara todo el hogar.
4. **Falta de auto-rebalanceo equitativo al unirse o salir integrantes**:
   - Cuando un nuevo integrante se une a un hogar existente, el backend le asignaba un porcentaje sin actualizar a los miembros previos, provocando que la suma de porcentajes en la base de datos superara el 100% (por ejemplo, 100% + 50% = 150%).
   - Al unirse un nuevo miembro o al salir uno, el sistema debe rebalancear automáticamente y de forma equitativa los porcentajes de todos los miembros activos para que siempre sumen exactamente el 100.0%.
5. **Falta de detalle de gastos y liquidaciones al tocar un ciclo en el Historial**:
   - Al presionar una tarjeta de ciclo mensual en el historial, no ocurre ninguna acción ni se muestran qué gastos o compras ocurrieron en ese período, qué pagos se hicieron ni quién quedó debiendo.
6. **Arrastre de saldos deudores pendientes al siguiente ciclo (Rollover)**:
   - Si un ciclo mensual concluye y un integrante no abonó ni liquidó su deuda con sus compañeros, ese saldo pendiente actualmente se pierde de la cuenta corriente del ciclo activo. Debe existir un mecanismo de arrastre (*rollover*) que traslade esa deuda no saldada al balance del ciclo en curso.
7. **Claridad en meses de menos de 31 días (Clamp de fechas de corte)**:
   - Los usuarios necesitan certeza de que si eligen el día 31 como fecha de corte, el sistema ajusta automáticamente en meses de 28, 29 (bisiesto) o 30 días, cerrando el ciclo en el último día real del mes sin errores de desbordamiento.
8. **Agrupación por días y meses con Scroll Infinito en transacciones**:
   - La lista de transacciones recientes actualmente se presenta plana. Debe organizarse visualmente por secciones: mes principal (ej. *Septiembre 2026*), subsecciones por día (ej. *Hoy - 5 de Septiembre*, *Ayer - 4 de Septiembre*), e incorporar carga progresiva / scroll infinito para soportar cientos de transacciones sin degradar la memoria ni la fluidez.

---

## What Changes
- **Backend: Sincronización Global de Gastos del Hogar**:
  - `GET /api/v1/expenses` devolverá tanto los gastos personales del usuario como los gastos de ámbito `HOUSEHOLD` del hogar activo al que pertenece.
- **Backend: Expulsión de Integrantes por Admin**:
  - Nuevo endpoint `DELETE /api/v1/households/members/:user_id` restringido al administrador.
- **Backend: Auto-Rebalanceo Equitativo a 100%**:
  - Al unirse un nuevo miembro (`POST /households/join`) o al removerse/abandonar un miembro (`DELETE /households/members/:user_id` o `POST /households/leave`), el usecase recalculará y actualizará de inmediato las cuotas de todos los integrantes activos para que sumen exactamente 100.0% (`100.0 / N`).
- **Backend: Detalle de Ciclo y Rollover de Deudas**:
  - Endpoint `GET /api/v1/households/cycles/:cycle_index` (o consulta por período) que retorne el detalle completo de gastos, pagos y participantes del ciclo.
  - Inclusión de saldos no saldados del ciclo inmediato anterior en el cálculo de `CalculateHouseholdBalance`.
- **Frontend Android: DTO de Balance Corregido y Tarjeta de Saldo Integral**:
  - Actualización de `HouseholdBalanceResponse` con `balances: List<RoomieBalanceDto>`, `total_household` y `settlement`.
  - Visualización en [HouseholdScreen.kt](file:///c:/Users/Jesus%20Osorio/projects/house-expenses/apps/mobile-android/app/src/main/java/com/houseexpenses/ui/screens/HouseholdScreen.kt) del Total de Gastos del Hogar, aporte pagado por cada roomie, su cuota esperada, su saldo neto (a favor / en contra) y la liquidación explicada en texto.
- **Frontend Android: Expulsión de Integrantes en la UI**:
  - Botón de expulsión en la lista de integrantes visible únicamente para el Admin, con diálogo de confirmación.
- **Frontend Android: Modal / Pantalla de Detalle de Ciclo Histórico**:
  - Al hacer clic en una tarjeta de ciclo del historial, desplegar un `ModalBottomSheet` con la lista de gastos de ese mes, desglose de pagos y estado de saldado.
- **Frontend Android: Agrupación por Mes/Día y Scroll Infinito**:
  - Agrupación jerárquica en listas de gastos: Encabezado de Mes > Encabezado de Día > Tarjetas de gastos.
  - Scroll infinito con carga perezosa paginada.

---

## Capabilities

### Modified Capabilities
- `household-management`: Expulsión de miembros por el admin, auto-rebalanceo a 100% en altas y bajas, y detalle de ciclos con rollover.
- `expenses`: Visibilidad cruzada de gastos del hogar para todos los miembros, y mapping correcto del balance de liquidación.
- `android-client`: Interfaz reactiva de balances multi-usuario, expulsión de miembros, modal de detalle de ciclos, y scroll infinito con agrupación por mes/día.

---

## Impact
- **Base de Datos**: No requiere cambios de esquema DDL; aprovecha los campos ya existentes en `HouseholdMember` y `Household`.
- **Compatibilidad API**: Totalmente retrocompatible; enriquece las respuestas de balance y sincronización sin romper firmas.
- **Seguridad**: Se valida estrictamente que solo el ADMIN pueda expulsar a otros integrantes y que ningún usuario pueda expulsarse a sí mismo a través del endpoint de expulsión (debe usar `leave`).
