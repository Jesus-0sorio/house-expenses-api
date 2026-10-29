# Tasks: Balance del Ciclo Hogar, Deserialización Resiliente de Fechas y Liquidación Directa

## 1. Backend Go: Deserialización Resiliente de Fechas y Liquidación Directa
- [x] 1.1 Implementar el tipo `FlexibleDate` con soporte para `"yyyy-MM-dd"`, RFC3339 y cadenas vacías en `sync_usecase.go`, aplicándolo a `SyncExpenseItem` y `SyncIncomeItem`.
- [x] 1.2 Refinar `CalculateHouseholdBalance` en `expense_usecase.go` para generar oraciones de liquidación directa con nombres explícitos de quién transfiere a quién (*"Debes pagar $X a [Nombre]"* / *"[Nombre] debe pagarte $X a ti"*).
- [x] 1.3 Añadir pruebas unitarias en `sync_usecase_test.go` enviando payloads JSON con fechas en formato plano `"2026-09-05"` y validar su correcto guardado en PostgreSQL con `go test ./...`.

## 2. Android UI: Consolidación de Balance del Ciclo y Canales de Pago en `HouseholdScreen.kt`
- [x] 2.1 Eliminar la tarjeta redundante externa de "Total Gastos Generales" ubicada sobre la lista de gastos en `HouseholdScreen.kt`.
- [x] 2.2 Consolidar la tarjeta de **Balance del Ciclo Activo**: mostrar el Total Gastos del Hogar con soporte de resiliencia local, el desglose de aportes y cuota por integrante (`"Pagó: $X • Cuota: $Y"`), y la caja de liquidación directa indicando a quién entregar el dinero.
- [x] 2.3 Restringir las opciones de canales en `SettlementPaymentDialog` exclusivamente a **"Transferencia"** y **"Efectivo"**.

## 3. Android UI: Limpieza de la Pantalla de Inicio en `DashboardScreen.kt`
- [x] 3.1 Retirar la subdivisión de columnas ("Total General Hogar" / "Mis Gastos Personales") de la tarjeta principal de gastos mensuales en `DashboardScreen.kt`.

## 4. Verificación y Pruebas
- [x] 4.1 Ejecutar `go test -v -count=1 ./...` y verificar que la suite completa de pruebas unitarias pase con 0 errores.
- [x] 4.2 Validar la integridad sintáctica de los componentes Kotlin modificados en la aplicación Android.
