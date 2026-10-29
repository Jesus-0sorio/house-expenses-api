# House Expenses - Cliente Móvil Android (Kotlin + Jetpack Compose)

Aplicación móvil nativa en Kotlin para el sistema **House Expenses & Personal Finance**.

## Características Implementadas

1. **Arquitectura Offline-First con Room SQLite**:
   - Permite registrar gastos e ingresos sin conexión a internet en supermercados o tiques físicos.
   - Base de datos local SQLite reactiva mediante `Flow` y StateFlow en Compose.
2. **Sincronización Automática en Segundo Plano (WorkManager)**:
   - Worker periódico (`PeriodicExpenseSync`) y por demanda (`ExpenseSyncWork`) con restricciones de red (`NetworkType.CONNECTED`).
   - Sube transacciones pendientes y sincroniza con el backend en Go vía `POST /api/v1/sync`.
3. **Escaneo de Facturas Electrónicas DIAN con Cámara (ML Kit + CameraX)**:
   - Detección en tiempo real de códigos QR oficiales de la DIAN (`NumFac`, `NitFac`, `ValTolFac`, `CUFE`).
   - Extracción inmediata del total y nombre de la tienda (D1, Éxito, Carulla, Jumbo, Ara).
   - Pre-llenado automático del formulario de gasto.
4. **UI Declarativa Moderna con Jetpack Compose y Material 3**:
   - Dashboard reactivo con contador de transacciones pendientes de sincronización.
   - Alternancia entre gasto personal y compartido del hogar (50/50).

## Instrucciones para Abrir y Ejecutar

1. Abre **Android Studio** (Koala / Iguana o superior).
2. Selecciona **Open** y navega a la carpeta: `c:\Users\Jesus Osorio\projects\house-expenses\apps\mobile-android`.
3. Espera a que Gradle sincronice las dependencias del proyecto.
4. Conecta un dispositivo Android físico o inicia un Emulador con Android 10+ (API 29+).
5. Pulsa **Run 'app'** (`Shift + F10`).

> **Nota para Emulador Android:** El cliente apunta por defecto a `http://10.0.2.2:8080/api/v1/` (alias de `localhost` en el emulador de Android Studio para comunicarse con la API en Go en tu computador).
