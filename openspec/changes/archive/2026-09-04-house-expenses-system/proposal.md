# Proposal: House Expenses & Personal Finance System

## Why
La gestión financiera compartida entre compañeros de casa (*roomies*) en Colombia suele ser caótica y propensa a errores cuando se realiza de manera manual mediante hojas de cálculo o notas compartidas. Particularmente, surgen cuatro puntos de dolor críticos:

1. **Fricción en el registro**: Las facturas de servicios públicos, arriendo y compras de supermercado llegan dispersas en correos electrónicos (como archivos `.zip` con XML de la DIAN y PDF) o en recibos físicos con código QR.
2. **Compras mixtas complejas**: En compras de supermercado conviven artículos de la casa (50/50) con artículos de consumo personal del comprador. La categorización manual ítem por ítem resulta tediosa.
3. **Falta de conciliación y duplicados**: Al pagar con tarjeta de crédito o débito, conviven los comprobantes bancarios (extractos) con las facturas electrónicas, lo que genera duplicidad de registros si no existe un motor de emparejamiento.
4. **Desconexión entre finanzas compartidas y personales**: No existe una solución que permita conciliar en un solo lugar el flujo de caja personal (ingresos fijos, primas, cuotas de créditos personales) con la cuota parte que le corresponde pagar del hogar.

## What Changes
Se implementará una plataforma integral compuesta por un **Backend Central en Go** con Arquitectura Hexagonal, un **Microservicio DIAN en Go** desacoplado, una **App Web** responsiva y una **App Móvil Android en Kotlin** (Offline-First):

* **Ingesta Automatizada DIAN & IMAP**:
  - **Microservicio Autónomo DIAN (`dian-service`)**: Servicio independiente encargado de consultar el catálogo web de la DIAN mediante códigos QR o CUFE, extraer la información estructurada de la factura electrónica (emisor, NIT, ítems, impuestos, total) y descargar la representación gráfica oficial (PDF).
  - Lector periódico de buzones de correo (Gmail/Outlook u otros) para extraer y procesar adjuntos `.zip` con XML UBL 2.1 y PDF.
  - Módulo de conciliación bancaria para cruzar extractos en PDF/Excel con facturas existentes, detectando duplicados de forma asistida o manual.
* **Modelo Dual Hogar (Household) vs. Modo Solitario**:
  - Espacio de finanzas personales 100% privado y confidencial para cada usuario.
  - Espacio compartido de Hogar con reparto configurable (50/50 para gastos fijos de arriendo, luz, agua, gas e internet).
  - Modo solitario adaptativo que desactiva preguntas de triaje si el usuario no pertenece a un hogar activo.
* **Triaje y Desglose Inverso Inteligente**:
  - Selector con lógica inversa para compras de supermercado: al marcar los pocos ítems personales, los restantes pasan automáticamente a la cuenta de la casa (o viceversa).
  - Configuración de comportamiento por defecto para comercios conocidos (ej. enviar compras de supermercados automáticamente al hogar sin interrupciones).
* **Gestión de Flujo de Caja e Ingresos**:
  - Registro de ingresos fijos (salario) e ingresos extraordinarios (primas semestrales legales, bonos, cesantías, freelance).
  - Control de cuotas de créditos personales y cálculo en tiempo real del saldo disponible mensual.
* **Entorno de Desarrollo Dual**:
  - Contenedorizado con Docker Compose (Backend principal Go con Air, Microservicio DIAN y PostgreSQL).
  - Ejecución nativa local ligera soportando SQLite embebido o Postgres local.

## Capabilities

- `expenses/identity`: Autenticación JWT, registro local y arquitectura OAuth2 extensible y desacoplada (iniciando con soporte para Gmail/Google y preparada para incorporar Hotmail/Microsoft o GitHub), gestión de usuarios, espacios de hogar (*households*) y preferencias adaptativas.
- `expenses/invoicing-dian`: Microservicio autónomo para consulta de factura electrónica en catálogo DIAN (QR / CUFE), descarga de representación gráfica (PDF), lector IMAP de correos y parser de XML UBL 2.1.
- `expenses/tracking`: Registro, desglose inverso, reclasificación de gastos fijos y variables, y liquidación de deudas entre roomies.
- `expenses/reconciliation`: Parser de extractos bancarios y motor heurístico de detección y resolución de duplicados.
- `expenses/personal-finance`: Registro de ingresos (fijos/extras), pasivos/cuotas mensuales y cálculo de flujo de caja.

## Impact
- **Rendimiento**: Backend en Go con bajo consumo de memoria (< 50MB RAM en reposo), ideal para correr en entornos gratuitos tipo Oracle Cloud Always Free o equipos locales de bajos recursos.
- **Privacidad y Seguridad**: Las finanzas personales (ingresos, extractos, cuotas personales) no son visibles para el roomie; únicamente se comparten los gastos explícitamente asignados al Hogar.
- **Conectividad Móvil**: Soporte Offline-First en Android para captura inmediata de gastos en la calle y sincronización reactiva en segundo plano cuando retorne la conexión.
