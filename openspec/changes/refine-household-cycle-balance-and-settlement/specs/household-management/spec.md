# Delta Spec: Household Management (Liquidación Directa y Canales Simplificados)

## ADDED Requirements

### Requirement: Resolución Directa de Liquidación entre Roomies
El texto de liquidación del balance del hogar (`settlement`) SHALL formularse desde una perspectiva directa y personalizada, indicando con claridad quién entrega el dinero a quién para cuadrar las cuentas.

#### Scenario: Usuario actual tiene saldo negativo (deudor)
- **GIVEN** que el usuario actual tiene un saldo neto de -$150.000 COP y otro roomie (ej. "Carlos") tiene saldo positivo de +$150.000 COP
- **WHEN** se presenta la liquidación sugerida
- **THEN** SHALL indicar de forma clara: `"Debes transferir $150.000 COP a Carlos para cuadrar cuentas."`

#### Scenario: Usuario actual tiene saldo positivo (acreedor)
- **GIVEN** que el usuario actual tiene un saldo neto a favor y otro roomie le adeuda
- **WHEN** se presenta la liquidación sugerida
- **THEN** SHALL indicar: `"Carlos debe transferirte $150.000 COP a ti para cuadrar cuentas."`

#### Scenario: Cuentas equilibradas
- **GIVEN** que las diferencias entre aportes y cuotas son menores a $1 COP
- **WHEN** se evalúa el balance
- **THEN** SHALL indicar: `"Las cuentas están equilibradas. ¡Nadie se debe nada!"`

### Requirement: Canales Exclusivos de Pago en Registro de Transferencias
El flujo de registro de pagos y transferencias entre roomies (`SettlementPaymentDialog`) SHALL permitir seleccionar únicamente entre dos canales de pago: **"Transferencia"** y **"Efectivo"**.

#### Scenario: Visualización de canales de pago en el modal
- **GIVEN** que el usuario abre el modal "Registrar Pago / Transferencia"
- **WHEN** se renderizan los chips o botones de medios de pago
- **THEN** SHALL mostrar exclusivamente las opciones "Transferencia" y "Efectivo"
- **AND** SHALL NO mostrar opciones de bancos específicos como "Nequi", "Daviplata" o "Bancolombia".
