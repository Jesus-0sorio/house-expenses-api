# Delta Spec: Household Management (Permisos de Edición y Formato de Liquidación)

## 1. Requisitos de Permisos en Gastos Compartidos
- **Propiedad de Mutación de Gastos**:
  - En la vista del hogar (`HouseholdScreen`), un usuario solo tiene permiso de edición y eliminación sobre los gastos compartidos que hayan sido registrados por él mismo (`exp.user_id == current_user_id`).
  - Si un gasto compartido fue pagado/creado por otro integrante del hogar, la tarjeta debe comportarse en modo de solo lectura: no debe ser interactiva para edición, no debe abrir el diálogo `EditExpenseDialog` y debe evidenciar que pertenece a otro miembro.

## 2. Requisitos de Formato de Liquidación (Balance en Pesos Colombianos)
- **Cálculo y Texto en Miles de COP**:
  - En las respuestas del backend y en la visualización móvil del balance del hogar, cualquier cifra monetaria expresada en la frase de liquidación (ej. *"X le debe a Y"*) debe presentarse sin centavos/decimales y con formato de miles estándar en Colombia (ej. `$ 20.000`, `$ 100.000`).
  - No deben emitirse cadenas que contengan montos como `$70000,00` ni `$20000.00 COP`.
