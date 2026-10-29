# Proposal: Modo Oscuro/Claro, Edición de Transacciones y Gestión de Hogares / Familia

## Why
La aplicación móvil Android cuenta con un flujo sólido de registro rápido y navegación estructurada, pero requiere tres capacidades esenciales para su usabilidad diaria:
1. **Confort Visual y Accesibilidad**: Permitir alternar entre tema oscuro (Dark) y claro (Light), organizando la jerarquía tipográfica y contrastes para que todos los textos sean legibles bajo cualquier condición de luz.
2. **Control y Corrección de Datos**: Permitir editar gastos e ingresos ya registrados (corregir monto, descripción, categoría o fecha) sin tener que reiniciar o perder el registro.
3. **Gestión Completa de Hogar / Familia**: Permitir a usuarios en modo solitario crear un hogar (generando código de invitación), unirse mediante un código de 6 caracteres, o ingresar directamente a través de un enlace profundo (Deep Link).

## What Changes
- **Soporte Dinámico de Temas (Dark / Light Theme)**:
  - Selector de tema (`DARK`, `LIGHT`, `SYSTEM`) persistido en preferencias.
  - Paletas semánticas completas en Material 3:
    - *Modo Oscuro*: Fondo `#0F172A`, superficies `#1E293B`, textos de alto contraste blanco/gris claro.
    - *Modo Claro*: Fondo `#F8FAFC`, tarjetas `#FFFFFF`, textos oscuros `#0F172A` y gris pizarra `#64748B`.
  - Botón de alternancia rápida en la barra superior.
- **Edición Completa de Gastos e Ingresos**:
  - Tocar cualquier gasto en el historial o en el dashboard abre un modal de edición con el monto formateado en COP, categoría y descripción.
  - Tocar cualquier ingreso en la pestaña de Flujo de Caja abre el modal para actualizar su monto, concepto o tipo (quincenal, mensual, extraordinario).
  - Sincronización transparente de las ediciones hacia el backend y Room SQLite.
- **Creación y Vinculación a Hogares / Familia**:
  - En la pestaña "Hogar":
    - Si no tiene hogar: tarjetas interactivas para "Crear Nuevo Hogar" o "Unirse con Código".
    - Si ya tiene hogar: visualización del código de invitación con botón para compartir y copiar enlace (`https://houseexpenses.app/join/CODE` o `houseexpenses://household/join?code=CODE`).
  - Soporte de Deep Links en Android mediante `IntentFilter` para unirse automáticamente con un solo clic.

## Capabilities

### `android-client`
- Soporte para alternar tema oscuro/claro y tokens de color dinámicos.
- Diálogos/pantallas de edición de transacciones (`ExpenseEntity` e `IncomeEntity`).
- Diálogos y llamadas API para crear y unirse a hogares (`POST /api/v1/households` y `POST /api/v1/households/join`).
- Configuración de Deep Links en el manifest de Android.

## Impact
- Elimina la rigidez de no poder corregir equivocaciones al digitar montos.
- Facilita la adopción entre parejas y roomies mediante enlaces de invitación directos.
- Proporciona una lectura descansada y profesional tanto de día como de noche.
