# Delta Spec: Android Client (Unificación de Balance y Limpieza Visual)

## ADDED Requirements

### Requirement: Eliminación de Tarjeta Redundante de Gastos Generales
En `HouseholdScreen.kt` NO se debe renderizar una tarjeta independiente de "Total Gastos Generales" por encima de la lista de gastos. La lista de transacciones del hogar SHALL iniciar inmediatamente después de las acciones del balance y el historial de ciclos.

#### Scenario: Visualización de la lista de gastos del hogar
- **GIVEN** que el usuario accede a la pantalla Hogar & Roomies
- **WHEN** se desplaza por la pantalla
- **THEN** SHALL visualizar directamente el encabezado `"Gastos del Hogar"` y los ítems correspondientes, sin ninguna tarjeta duplicada previa.

### Requirement: Presentación Integral del Total y Cuotas en la Tarjeta de Balance
La tarjeta `"Balance del Ciclo Activo"` SHALL concentrar todas las métricas agregadas del ciclo:
1. **Total Gastos del Hogar**: Valor consolidado en COP (tomando el valor del backend o el cálculo de fallback con los gastos locales si el backend aún no ha sincronizado).
2. **Aporte y Cuota por Integrante**: Para cada integrante del hogar, mostrar tanto lo que pagó (`Pagó: $X`) como lo que le correspondía pagar (`Cuota: $Y`), junto a su porcentaje de reparto (`Z%`).
3. **Estado de Cuenta Neto**: Badge indicando si está al día, tiene saldo a favor (`+$X a favor`) o adeuda (`-$X debe`).
4. **Liquidación Personalizada**: Indicar expresamente quién debe pagar a quién y a nombre de quién se realiza el pago.

#### Scenario: Visualización de tarjeta de balance con cuotas claras
- **GIVEN** un hogar con 2 integrantes con reparto 50/50 y un total de gastos de $400.000 COP, donde el usuario pagó $300.000 COP y el roomie pagó $100.000 COP
- **WHEN** el usuario visualiza la tarjeta de Balance del Ciclo Activo
- **THEN** el Total Gastos del Hogar SHALL mostrar `$ 400.000 COP`
- **AND** para el usuario SHALL indicar `Pagó: $300.000 • Cuota: $200.000 (50.0%)` con badge `+$100.000 a favor`
- **AND** para el roomie SHALL indicar `Pagó: $100.000 • Cuota: $200.000 (50.0%)` con badge `-$100.000 debe`
- **AND** la caja de liquidación SHALL indicar que el roomie debe transferirle $100.000 COP al usuario.

### Requirement: Retiro de Subdivisión de Columnas en Pantalla de Inicio (Dashboard)
En `DashboardScreen.kt`, la tarjeta superior de gastos del mes actual SHALL mantener su diseño original limpio enfocado en los gastos del mes del usuario y el estado de sincronización, sin incorporar columnas adicionales de desglose ("Total General Hogar" / "Mis Gastos Personales") que presenten números dispares.

#### Scenario: Visualización de tarjeta principal en Inicio
- **GIVEN** que el usuario abre la pantalla de Inicio (Dashboard)
- **WHEN** observa la tarjeta superior `"Gastos de [Mes]"`
- **THEN** SHALL ver el monto total acumulado del mes y el estado de sincronización
- **AND** SHALL NO contener la fila con divisiones `"Total General Hogar"` ni `"Mis Gastos Personales"`.
