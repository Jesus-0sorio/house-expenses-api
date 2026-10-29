# Spec Delta: expenses

## MODIFIED REQUIREMENTS

### Requirement: Desacoplamiento de Pagos de Liquidación y Gastos del Hogar
El registro de gastos del hogar (`Expense` con `scope = "HOUSEHOLD"`) SHALL contener exclusivamente compras, suministros y servicios comunes del hogar (ej. mercado, energía, agua, internet).
- Ningún pago para saldar deudas o transferir dinero a un roomie SHALL guardarse como gasto de categoría `"Servicios"` o gasto de hogar.
- Las transferencias entre integrantes para cuadrar cuentas SHALL gestionarse independientemente mediante la entidad `SettlementPayment`.

### Requirement: Sincronización Batch con Soporte para Pagos
El endpoint `POST /api/v1/sync` SHALL extender su contrato para aceptar `payments: []SettlementPaymentItem`:
- El backend procesará y almacenará los pagos de liquidación recibidos en el lote de sincronización con idempotencia por `id`/`client_id`.
- La respuesta de sincronización incluirá `synced_payments: []SyncItemAck`.
