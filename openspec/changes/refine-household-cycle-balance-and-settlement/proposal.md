# Proposal: Corrección de Balance del Ciclo Hogar, Resolución de Liquidación Directa, Deserialización Resiliente de Fechas y Simplificación de Pagos

## Why
Tras la última iteración y validación con el usuario, se identificaron cuatro discrepancias funcionales y visuales críticas en el sistema de hogar y balance compartido:

1. **El Balance del Ciclo del Hogar y los Aportes por Integrante aparecen en $0**:
   - **Causa Raíz Técnica**: Al registrar un gasto en Android, la fecha se envía como cadena en formato estándar ISO local `"yyyy-MM-dd"` (ejemplo: `"2026-09-05"`).
   - En el backend Go (`sync_usecase.go`), `SyncExpenseItem` define `Date time.Time`. El deserializador por defecto de Gin (`json.Unmarshal`) exige estrictamente el formato RFC3339 (`"2006-01-02T15:04:05Z07:00"`).
   - Al recibir `"2026-09-05"`, Gin falla con un error `400 Bad Request` (`parsing time "2026-09-05" as "2006-01-02T15:04:05Z07:00": cannot parse "" as "T"`).
   - Por esta razón, el backend rechaza silenciosamente la sincronización en lote, los gastos compartidos nunca se insertan en PostgreSQL y, por consiguiente, el endpoint de balance (`/api/v1/household/balance`) calcula sobre una lista vacía de gastos, retornando `total_household: 0` y `total_paid: 0` para todos los roomies.

2. **Creación Innecesaria de una Card Externa de "Total Gastos Generales"**:
   - Se creó una tarjeta separada adicional sobre la lista de gastos que satura la pantalla.
   - El usuario especificó que **no se debe crear una tarjeta nueva**, sino que ese valor del total acumulado debe residir naturalmente como encabezado principal dentro de la tarjeta de **Balance del Ciclo Hogar**.

3. **Falta de Claridad en la Liquidación y Cuotas por Integrante**:
   - En el balance del ciclo actual debe quedar explícito:
     - El valor total de gastos del ciclo de la casa.
     - Cuánto ha pagado cada integrante y cuánto le correspondía pagar según su porcentaje (su cuota real esperada).
     - **A quién exactamente se le debe entregar el dinero para cuadrar cuentas** (por ejemplo: *"Tú debes pagar $162.500 a Carlos"* o *"Carlos debe pagarte $162.500 a ti"*), en lugar de frases genéricas o impersonales.

4. **Columna con Datos Incorrectos en la Pantalla de Inicio (Dashboard)**:
   - Se añadió una subdivisión de columnas ("Total General Hogar" vs "Mis Gastos Personales") dentro de la tarjeta superior mensual en `DashboardScreen.kt` que confunde las métricas y muestra cifras discrepantes al no coincidir con el ciclo facturable. Esta sección adicional debe retirarse.

5. **Opciones Excesivas en Registro de Pagos / Transferencias**:
   - El modal actual ofrece múltiples canales bancarios ("Nequi", "Daviplata", "Bancolombia", "Efectivo").
   - El usuario requiere exclusivamente dos únicas opciones: **"Transferencia"** o **"Efectivo"**.

---

## What Changes

### 1. Backend Go: Deserialización Resiliente de Fechas en Sincronización Batch
- Implementar un tipo de fecha flexible (`FlexibleDate` o deserializador tolerante) en `SyncExpenseItem` y `SyncIncomeItem` que acepte indistintamente:
  - Formato de fecha plana `"yyyy-MM-dd"` (`"2006-01-02"`).
  - Formato estándar ISO / RFC3339 (`"2006-01-02T15:04:05Z"`).
  - Valores nulos o vacíos (usando `time.Now()` como fallback seguro).
- Con esto, las peticiones `POST /api/v1/sync` desde la app Android se procesarán con HTTP 201 Created y los gastos se persistirán correctamente en PostgreSQL vinculados al hogar.
- Optimizar `CalculateHouseholdBalance` para generar sentencias de liquidación con perspectiva directa hacia el usuario en sesión (*"Tú le debes $X a [Roomie]"* o *"[Roomie] te debe $X a ti"*).

### 2. Frontend Android: Corrección de UI en `HouseholdScreen.kt`
- **Eliminar la tarjeta redundante** de "Total Gastos Generales" que se agregó antes de la lista de gastos.
- **Enriquecer la tarjeta de "Balance del Ciclo Activo"**:
  - Encabezado con el **Total Gastos del Hogar** consolidado en COP.
  - Desglose por integrante: nombre, cuánto pagó (`Pagó: $X`) y su cuota o valor que debe poner (`Cuota: $Y`).
  - Saldo neto individual con badge destacado (*"+$X a favor"* o *"-$X debe"*).
  - Caja de liquidación directa indicando exactamente **a quién se le debe transferir o entregar el dinero para cuadrar** (*"Debes pagar $X a [Nombre]"*).
  - Soporte de fallback local inmediato: si el balance del servidor aún está en 0 pero existen gastos locales del hogar, calcular reactivamente el total y las cuotas a partir de los datos en Room para evitar mostrar $0.
- **Simplificación del Modal `SettlementPaymentDialog`**:
  - Restringir los canales de pago a solo dos opciones: **"Transferencia"** y **"Efectivo"**.

### 3. Frontend Android: Limpieza de la Tarjeta Principal en `DashboardScreen.kt`
- Retirar la fila divisoria y columnas añadidas en la tarjeta superior del mes, restaurando el diseño original limpio de gastos del mes personal.
- Mantener en `DashboardScreen` la tarjeta dedicada al "Balance del Hogar" sincronizada y precisa.

---

## Capabilities

### Modified Capabilities
- `expenses`: Deserialización de fechas tolerante a formatos `"yyyy-MM-dd"` y RFC3339 en endpoints de sincronización batch, garantizando persistencia en PostgreSQL sin rechazos HTTP 400.
- `household-management`: Cálculo unificado de ciclo con liquidación directa (quién le paga a quién) y canales de pago simplificados ("Transferencia" / "Efectivo").
- `android-client`: Interfaz de balance de ciclo integrada sin tarjetas redundantes, eliminación de métricas inconsistentes en pantalla de inicio y visualización precisa de aportes por integrante.

---

## Impact
- Se garantiza la persistencia efectiva de los gastos enviados desde Android hacia el backend Go.
- El usuario ve reflejado inmediatamente el total acumulado del ciclo y cuánto debe pagar cada integrante dentro del Balance del Ciclo Hogar.
- Se elimina la confusión visual generada por la tarjeta extra y la columna en el inicio.
