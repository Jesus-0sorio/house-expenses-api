# 🏠 House Expenses API — Microservices & Backend Engine

Backend empresarial desarrollado en **Go (Golang 1.23)** basado en **Arquitectura Hexagonal (Ports & Adapters)** para la gestión inteligente de finanzas personales, presupuestos compartidos del hogar (*Households*), conciliación bancaria y consulta automatizada de facturas electrónicas de la **DIAN (Colombia)** con sincronización *offline-first*.

---

## 🏗️ Arquitectura del Sistema

El proyecto sigue una separación estricta de responsabilidades bajo los principios de Clean Architecture / Hexagonal Architecture:

```
house-expenses/
├── cmd/
│   └── api/                # Punto de entrada principal (main.go)
├── internal/
│   ├── core/               # Núcleo de la aplicación (100% independiente de frameworks)
│   │   ├── domain/         # Entidades de negocio (Expense, Household, Invoice, User, etc.)
│   │   ├── ports/          # Interfaces (Repositories, DIAN Client, OAuth, Statement Parser)
│   │   └── usecase/        # Casos de uso de negocio (Auth, Cashflow, Sync, Invoice, etc.)
│   └── adapters/           # Infraestructura y tecnologías externas
│       ├── handlers/       # Controladores HTTP (Gin Framework) y Middlewares
│       ├── repository/     # Implementaciones de persistencia dual: PostgreSQL 16 y SQLite
│       ├── dian_client/    # Cliente HTTP y Parser UBL para la DIAN
│       ├── statement/      # Parser de extractos bancarios
│       └── oauth/          # Proveedores de autenticación (Google OAuth 2.0)
└── services/
    └── dian-service/       # Microservicio autónomo para consultar el catálogo VPFE de la DIAN
```

---

## 🚀 Características Principales

1. **Gestión de Hogares y División de Gastos (Households):**
   - Creación de hogares compartidos, balance entre miembros y resolución de liquidaciones (*Settlements*).
   - Control de permisos granulares por miembro.
2. **Facturación Electrónica DIAN (Colombia):**
   - Microservicio dedicado (`dian-service`) con scraping/API al catálogo VPFE (`catalogo-vpfe.dian.gov.co`).
   - Parseo de documentos XML estándar UBL para extracción automática de ítems, IVA y totales.
3. **Sincronización Offline-First (Sync Engine):**
   - Endpoint por lotes (`/api/v1/sync/batch`) diseñado para sincronizar transacciones creadas sin internet en clientes móviles y web.
4. **Persistencia Dual (PostgreSQL & SQLite):**
   - Configurable mediante variable de entorno `DB_DRIVER`: utiliza **PostgreSQL 16** en producción/Docker o **SQLite** para desarrollo local sin dependencias.
5. **Conciliación Bancaria y Cashflow:**
   - Análisis de flujo de caja (*Cashflow*) mensual y parser de extractos bancarios para conciliar gastos reales vs. registrados.
6. **Autenticación y Seguridad:**
   - Tokens JWT criptográficos y flujo de inicio de sesión con Google OAuth 2.0.

---

## 🛠️ Tecnologías Utilizadas

* **Lenguaje:** Go (Golang) 1.23+
* **Framework Web:** Gin Web Framework
* **Bases de Datos:** PostgreSQL 16 (Producción) / SQLite3 (Desarrollo local)
* **Contenedores:** Docker & Docker Compose
* **Live Reload:** Air (`.air.toml`)
* **Autenticación:** JWT (JSON Web Tokens) & Google OAuth2
* **Arquitectura:** Hexagonal / Clean Architecture con pruebas unitarias

---

## ⚙️ Variables de Entorno (`.env`)

Copia el archivo de ejemplo para configurar tu entorno:

```bash
cp .env.example .env
```

| Variable | Descripción | Valor por Defecto / Ejemplo |
| :--- | :--- | :--- |
| `PORT` | Puerto HTTP del servidor | `8080` |
| `GIN_MODE` | Modo de ejecución de Gin | `debug` o `release` |
| `DB_DRIVER` | Driver de base de datos (`postgres` o `sqlite`) | `postgres` |
| `DB_HOST` | Host de PostgreSQL | `localhost` (o `postgres` en Docker) |
| `DB_PORT` | Puerto de PostgreSQL | `5432` |
| `DB_USER` | Usuario de base de datos | `expenses` |
| `DB_PASSWORD` | Contraseña de base de datos | `expenses_secret` |
| `DB_NAME` | Nombre de la base de datos | `house_expenses` |
| `JWT_SECRET` | Clave secreta para firmar tokens JWT | *Cadena segura* |
| `DIAN_SERVICE_URL` | URL del microservicio DIAN | `http://localhost:8081` |
| `GOOGLE_CLIENT_ID` | Client ID de Google Console | *Opcional* |
| `GOOGLE_CLIENT_SECRET` | Client Secret de Google | *Opcional* |

---

## 🐳 Puesta en Marcha Rápida con Docker Compose

La forma más rápida de levantar toda la infraestructura (API + DIAN Service + PostgreSQL):

```bash
# 1. Clonar el repositorio
git clone https://github.com/Jesus-0sorio/house-expenses-api.git
cd house-expenses-api

# 2. Iniciar los servicios
docker-compose up -d --build

# 3. Verificar estado de los servicios
docker-compose ps
```

* **API Principal:** `http://localhost:8080`
* **Microservicio DIAN:** `http://localhost:8081`
* **PostgreSQL:** `localhost:5432`

---

## 💻 Ejecución Local (Sin Docker)

Si prefieres ejecutar directamente con Go y SQLite:

```bash
# Instalar dependencias
go mod download

# Ejecutar con Air (Hot-Reload)
air

# O ejecutar directamente con Go
go run cmd/api/main.go
```

---

## 🧪 Pruebas Unitarias

El proyecto incluye tests unitarios para los casos de uso principales:

```bash
# Ejecutar todas las pruebas unitarias
go test ./... -v
```

---

## 📱 Ecosistema de Clientes

Este backend alimenta los siguientes clientes independientes:

* **[house-expenses-mobile](https://github.com/Jesus-0sorio/house-expenses-mobile):** Aplicación nativa Android (Kotlin, Jetpack Compose, Room, Offline Sync).
* **[house-expenses-web](https://github.com/Jesus-0sorio/house-expenses-web):** Cliente web administrativo (React, Vite, TypeScript, Tailwind CSS).

---

## 📄 Licencia

Este proyecto está bajo la Licencia MIT.
