# Delta Spec: Gestión del Hogar - Visualización de Historial de Ciclos

## 1. Requisitos Modificados

### REQ-HOUSEHOLD-CYCLE-01: Limpieza del Historial de Ciclos
- **Comportamiento Anterior**: En `HouseholdScreen`, las tarjetas del historial mostraban el texto `"Ciclo #$cycleIndex: $start_date al $end_date"`, y el BottomSheet mostraba `"Detalle del Ciclo #$cycleIndex"`.
- **Nuevo Comportamiento**:
  - Se elimina la mención técnica del índice de ciclo (`"Ciclo #0"`, `"Ciclo #1"`).
  - En la lista del historial de ciclos, la cabecera de cada tarjeta debe mostrar únicamente el rango de fechas formateado: `"${c.start_date} al ${c.end_date}"`.
  - En el diálogo/BottomSheet de detalle del ciclo, el encabezado debe mostrar `"${detail.start_date} al ${detail.end_date}"` (o `"Detalle del Ciclo"` como título principal con el rango como subtítulo destacado).
  - Se conservan todos los valores relevantes: estado (`ACTIVO`, `CERRADO`), total gastado en el ciclo, desglose de aportes por integrante y saldo neto.
