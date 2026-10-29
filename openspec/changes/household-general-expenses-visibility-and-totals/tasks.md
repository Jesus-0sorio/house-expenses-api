## 1. Backend Go: Resiliencia de Sincronización Batch
- [x] 1.1 Modificar `SyncExpenseItem` en `sync_usecase.go` para aceptar `ClientID` o `ID` (removiendo `binding:"required"` rígido sobre `ClientID`), y garantizar la asignación de `user.ActiveHouseholdID` a gastos compartidos.
- [x] 1.2 Añadir y validar prueba unitaria en `sync_usecase_test.go` verificando sincronización exitosa de gastos usando tanto `id` como `client_id`.

## 2. Android Data Layer: Corrección de DTOs y Sincronización Inmediata
- [x] 2.1 Actualizar `SyncBatchDto.kt` para incorporar `SyncItemAckDto`, actualizar `SyncResponse` para soportar confirmaciones estructuradas sin `JsonSyntaxException`, y añadir `@SerializedName("client_id")` en `ExpenseSyncDto`.
- [x] 2.2 Actualizar `performSync()` en `ExpenseRepository.kt` para procesar la lista estructurada de acks e insertar los gastos compartidos remotos vinculando `householdId`.
- [x] 2.3 Actualizar `createExpense` y `updateExpense` en `ExpenseViewModel.kt` para asignar `householdId = if (scope == "HOUSEHOLD") sessionManager.activeHouseholdId else null` y ejecutar `repository.performSync()` inmediatamente en una corrutina.

## 3. Android Presentation Layer: Vista Unificada de Gastos Generales y Pagadores
- [x] 3.1 Implementar en `HouseholdScreen.kt` una tarjeta destacada con el **Total de Gastos Generales del Hogar** en COP que sume todos los gastos del ciclo de todos los integrantes.
- [x] 3.2 Rediseñar la lista de gastos en `HouseholdScreen.kt` para mostrar de forma destacada el integrante que pagó o registró cada compra ("Pagado por: Tú" en verde o "Pagado por: [Nombre]" en púrpura).
- [x] 3.3 Mostrar el nombre del integrante pagador en las tarjetas de gasto del hogar dentro de `ExpensesListScreen.kt`.
- [x] 3.4 Actualizar la tarjeta principal de `DashboardScreen.kt` para desglosar el total general de la casa respecto a los gastos personales.

## 4. Verificación y Pruebas
- [x] 4.1 Ejecutar `go test ./...` y verificar que todas las pruebas unitarias del backend pasen con éxito.
- [x] 4.2 Validar la sincronización cruzada registrando un gasto con un usuario y comprobando su visualización con pagador y total en otro usuario del mismo hogar.
