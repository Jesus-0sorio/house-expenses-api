# Spec Delta: household-management

## ADDED REQUIREMENTS

### Requirement: Registro y Consulta de Pagos de Liquidación entre Integrantes
El sistema SHALL permitir registrar transferencias y pagos directos entre dos miembros de un mismo hogar para saldar deudas del ciclo (`SettlementPayment`).
- Cada pago SHALL registrar:
  - `id`: Identificador único UUID.
  - `household_id`: Identificador del hogar al que pertenece la transacción.
  - `payer_user_id`: Identificador del usuario que transfiere o entrega el dinero.
  - `receiver_user_id`: Identificador del usuario que recibe el dinero.
  - `amount`: Monto en pesos colombianos (COP), estrictamente mayor a cero.
  - `channel`: Medio utilizado ("Transferencia" o "Efectivo").
  - `date`: Fecha de la transacción.
  - `notes`: Descripción o nota informativa opcional.
- El sistema SHALL exponer los endpoints:
  - `POST /api/v1/households/payments`: Registra una nueva transferencia de liquidación.
  - `GET /api/v1/households/payments`: Retorna el historial de pagos del ciclo activo del hogar.

### Requirement: Compensación de Saldo Neto con Pagos de Liquidación
El caso de uso `CalculateHouseholdBalance` SHALL incluir las transferencias de liquidación en el balance de cada integrante:
- Los pagos de liquidación NO SHALL sumar al `total_household` de gastos de compras del hogar.
- El saldo neto de cada integrante se calculará como:
  $$\text{net\_balance}_u = (\text{total\_paid\_expenses}_u - \text{expected\_share}_u) + \text{payments\_sent}_u - \text{payments\_received}_u + \text{rollover}_u$$
- Las oraciones de liquidación SHALL reflejar la deuda remanente después de deducir los pagos ya efectuados en el ciclo.

### Requirement: Recuperación Integral de Estado del Hogar
El sistema SHALL exponer `GET /api/v1/households/current` retornando la información completa del hogar del usuario autenticado:
- `id`: UUID del hogar.
- `name`: Nombre descriptivo (ej. "Casa").
- `invite_code`: Código alfanumérico de invitación (8 caracteres).
- `billing_cycle_day`: Día de corte (1 a 31).
- `role`: Rol del usuario en el hogar ("ADMIN" o "MEMBER").
- `members`: Lista de integrantes con sus perfiles y porcentajes de repartición.
