# Slate — Backend REST API

> Sistema de gestión de fiados y control de deudas para kioscos y comercios de barrio.

[![Java](https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring Security](https://img.shields.io/badge/Spring%20Security-JWT-6DB33F?logo=springsecurity&logoColor=white)](https://spring.io/projects/spring-security)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15+-336791?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Maven](https://img.shields.io/badge/Maven-3.8+-C71A36?logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![JUnit 5](https://img.shields.io/badge/Tests-JUnit%205%20%2B%20Mockito-25A162?logo=junit5&logoColor=white)](https://junit.org/junit5/)

---

## Descripción

**Slate** es una API REST desarrollada con Java 21 y Spring Boot que permite a comerciantes registrar y gestionar fiados (créditos informales) y pagos de sus clientes. Incluye:

- **Autenticación segura** con JWT (registro e inicio de sesión).
- **Gestión de clientes** asociados al comerciante autenticado (multi-tenant por diseño).
- **Registro de transacciones** del tipo `FIADO` o `PAGO` por cliente.
- **Cálculo automático de saldo** (`totalBalance = Σ FIADO − Σ PAGO`).
- **Endpoint "Quién me debe"** con lista consolidada de deudores ordenada por monto adeudado.
- **Motor de puntaje de riesgo** que clasifica a cada cliente en `BAJO`, `MEDIO` o `ALTO`.

---

## Stack Tecnológico

| Capa | Tecnología |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot 4.1.1 |
| Seguridad | Spring Security + JJWT 0.12.5 |
| Persistencia | Spring Data JPA / Hibernate |
| Base de datos | PostgreSQL 15+ |
| Variables de entorno | spring-dotenv (`.env`) |
| Build | Maven 3.8+ / Maven Wrapper |
| Testing | JUnit 5 + Mockito + AssertJ |

---

## Requisitos Previos

Asegurate de tener instalado en tu entorno de desarrollo:

- **Java 21 JDK** — [Descargar OpenJDK 21](https://openjdk.org/projects/jdk/21/)
- **PostgreSQL 15+** — instancia corriendo localmente
- **Maven 3.8+** — o usar el Maven Wrapper incluido (`./mvnw`)

> No es necesario instalar Maven globalmente. El proyecto incluye `mvnw` / `mvnw.cmd`.

---

## Instalación y Configuración

### 1. Clonar el repositorio

```bash
git clone https://github.com/DannyOkk/slate-backend.git
cd slate-backend
```

### 2. Crear la base de datos en PostgreSQL

Conectate a tu instancia de PostgreSQL y ejecutá:

```sql
CREATE DATABASE slate_db;
```

### 3. Configurar las variables de entorno

Copiá el archivo de ejemplo y completá los valores:

```bash
cp .env.example .env
```

Luego editá `.env` con tus credenciales:

```env
DB_URL=jdbc:postgresql://localhost:5432/slate_db
DB_USER=postgres
DB_PASSWORD=tu_contraseña_segura

JWT_SECRET=clave_secreta_para_firmar_tokens_jwt_minimo_256_bits
JWT_EXPIRATION=86400000
```

> [!IMPORTANT]
> `JWT_SECRET` debe tener **al menos 256 bits** (32 caracteres) para cumplir con el algoritmo HS256 usado por JJWT. Podés generar uno seguro con:
> ```bash
> openssl rand -base64 64
> ```

| Variable | Descripción | Ejemplo |
|---|---|---|
| `DB_URL` | URL de conexión JDBC a PostgreSQL | `jdbc:postgresql://localhost:5432/slate_db` |
| `DB_USER` | Usuario de la base de datos | `postgres` |
| `DB_PASSWORD` | Contraseña del usuario | `mi_password` |
| `JWT_SECRET` | Clave secreta para firmar los tokens JWT | `super_secret_key_256bits...` |
| `JWT_EXPIRATION` | Expiración del token en milisegundos | `86400000` *(24 horas)* |

### 4. Compilar y ejecutar los tests

```bash
# Linux / macOS
./mvnw clean test

# Windows
mvnw.cmd clean test
```

> [!NOTE]
> Los tests de integración requieren que la base de datos PostgreSQL esté corriendo y accesible con las credenciales configuradas en `.env`.

Salida esperada:

```
[INFO] Tests run: 23, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### 5. Iniciar la aplicación

```bash
# Linux / macOS
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run
```

La API queda disponible en: **`http://localhost:8080`**

---

## Estructura del Proyecto

```
slate-backend/
├── src/
│   ├── main/
│   │   ├── java/com/slate/backend/
│   │   │   ├── config/           # SecurityConfig, configuración de beans
│   │   │   ├── controller/       # AuthController, ClientController, TransactionController
│   │   │   ├── dto/              # Request/Response DTOs
│   │   │   ├── entity/           # User, Client, Transaction, enums
│   │   │   ├── exception/        # Manejo global de excepciones
│   │   │   ├── repository/       # UserRepository, ClientRepository, TransactionRepository
│   │   │   ├── security/         # JwtAuthenticationFilter, JwtService
│   │   │   ├── service/          # AuthService, ClientService, TransactionService, RiskScoreService
│   │   │   └── SlateBackendApplication.java
│   │   └── resources/
│   │       └── application.yaml  # Configuración de la app (usa variables de .env)
│   └── test/
│       └── java/com/slate/backend/
│           ├── controller/       # ClientControllerTest, TransactionControllerTest
│           └── service/          # ClientServiceTest, TransactionServiceTest, RiskScoreServiceTest
├── .env.example                  # Plantilla de variables de entorno
├── ROADMAP.md                    # Sprints y tareas del proyecto
├── pom.xml
└── mvnw / mvnw.cmd
```

---

## API Reference

Todos los endpoints requieren autenticación JWT, excepto `POST /api/auth/register` y
`POST /api/auth/login`.

**Header requerido para endpoints protegidos:**
```
Authorization: Bearer <token>
```

---

### Autenticación — `/api/auth`

Endpoints públicos. No requieren token.

| Método | Endpoint | Descripción |
|---|---|---|
| `POST` | `/api/auth/register` | Registra un nuevo comerciante |
| `POST` | `/api/auth/login` | Autentica y devuelve un token JWT |

#### `POST /api/auth/register`

**Request body:**
```json
{
  "name": "Juan García",
  "email": "juan@kiosco.com",
  "password": "password123",
  "phone": "+54 9 11 1234-5678"
}
```

**Response `201 Created`:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

| Código | Descripción |
|---|---|
| `201` | Registro exitoso. Devuelve el JWT. |
| `409` | El email ya está registrado. |

---

#### `POST /api/auth/login`

**Request body:**
```json
{
  "email": "juan@kiosco.com",
  "password": "password123"
}
```

**Response `200 OK`:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

| Código | Descripción |
|---|---|
| `200` | Login exitoso. Devuelve el JWT. |
| `401` | Credenciales incorrectas. |

---

#### `GET /api/auth/profile`

Requiere un JWT válido y devuelve los datos personales del usuario
autenticado, sin incluir la contraseña.

**Response `200 OK`:**
```json
{
  "id": 1,
  "name": "Juan García",
  "email": "juan@kiosco.com",
  "phone": "+54 9 11 1234-5678"
}
```

---

#### `PUT /api/auth/profile`

Requiere un JWT válido. Actualiza únicamente los datos personales del usuario
autenticado.

**Request body:**
```json
{
  "name": "Nuevo nombre",
  "email": "nuevo@email.com",
  "phone": "+54 9 11 1234-5678"
}
```

**Response `200 OK`:**
```json
{
  "id": 1,
  "name": "Nuevo nombre",
  "email": "nuevo@email.com",
  "phone": "+54 9 11 1234-5678"
}
```

La respuesta nunca incluye la contraseña. Si cambia el email, el JWT actual
deja de ser válido y se debe iniciar sesión nuevamente para obtener un token
actualizado.

---

#### `PUT /api/auth/password`

Requiere un JWT válido y cambia la contraseña del usuario autenticado.

**Request body:**
```json
{
  "currentPassword": "contraseña-actual",
  "newPassword": "nueva-contraseña"
}
```

Responde `204 No Content` cuando el cambio es exitoso. La nueva contraseña
debe tener al menos seis caracteres, ser diferente de la actual y nunca se
devuelve ni se registra.

---

### Clientes — `/api/clients`

Todos los endpoints de clientes son privados. Cada comerciante solo puede ver y operar sus propios clientes.

| Método | Endpoint | Descripción |
|---|---|---|
| `GET` | `/api/clients` | Lista todos los clientes del comerciante autenticado |
| `POST` | `/api/clients` | Crea un nuevo cliente |
| `GET` | `/api/clients/{id}` | Detalle de un cliente (incluye saldo y riesgo) |
| `PUT` | `/api/clients/{id}` | Actualiza los datos de un cliente |
| `DELETE` | `/api/clients/{id}` | Elimina un cliente |
| `GET` | `/api/clients/debtors` | Lista de deudores ordenada por saldo adeudado |

#### `GET /api/clients`

**Response `200 OK`:**
```json
[
  {
    "id": 1,
    "name": "María López",
    "phone": "+54 9 11 9876-5432",
    "address": "Av. Rivadavia 1234",
    "notes": "Cliente frecuente",
    "createdAt": "2026-10-01T10:00:00",
    "totalBalance": 1500.00,
    "riskLevel": "MEDIO"
  }
]
```

---

#### `POST /api/clients`

**Request body:**
```json
{
  "name": "María López",
  "phone": "+54 9 11 9876-5432",
  "address": "Av. Rivadavia 1234",
  "notes": "Cliente frecuente"
}
```

**Response `201 Created`:** mismo esquema que `GET /api/clients/{id}`.

---

#### `GET /api/clients/{id}`

**Response `200 OK`:**
```json
{
  "id": 1,
  "name": "María López",
  "phone": "+54 9 11 9876-5432",
  "address": "Av. Rivadavia 1234",
  "notes": "Cliente frecuente",
  "createdAt": "2026-10-01T10:00:00",
  "totalBalance": 1500.00,
  "riskLevel": "MEDIO"
}
```

| Código | Descripción |
|---|---|
| `200` | Cliente encontrado. |
| `404` | Cliente no encontrado o no pertenece al usuario. |

---

#### `PUT /api/clients/{id}`

**Request body:** mismo esquema que `POST /api/clients`.
**Response `200 OK`:** cliente actualizado.

---

#### `DELETE /api/clients/{id}`

**Response `204 No Content`** — sin body.

---

#### `GET /api/clients/debtors`

Retorna los clientes con saldo deudor (`totalBalance > 0`), ordenados de mayor a menor deuda.

**Query params:**

| Parámetro | Tipo | Default | Descripción |
|---|---|---|---|
| `minBalance` | `decimal` | `0.01` | Saldo mínimo para aparecer en la lista |

**Response `200 OK`:**
```json
[
  {
    "clientId": 3,
    "clientName": "Pedro Ramírez",
    "clientPhone": "+54 9 11 5555-0000",
    "totalBalance": 8500.00,
    "lastTransactionDate": "2026-10-20T18:30:00"
  },
  {
    "clientId": 1,
    "clientName": "María López",
    "clientPhone": "+54 9 11 9876-5432",
    "totalBalance": 1500.00,
    "lastTransactionDate": "2026-10-15T09:00:00"
  }
]
```

---

### Transacciones — `/api/clients/{clientId}/transactions`

| Método | Endpoint | Descripción |
|---|---|---|
| `POST` | `/api/clients/{clientId}/transactions` | Registra un fiado o pago |
| `GET` | `/api/clients/{clientId}/transactions` | Historial con saldo total acumulado |

#### `POST /api/clients/{clientId}/transactions`

Registra un movimiento para el cliente especificado.

**Request body:**
```json
{
  "amount": 500.00,
  "type": "FIADO",
  "description": "Mercadería del martes"
}
```

| Campo | Tipo | Valores válidos | Requerido |
|---|---|---|---|
| `amount` | `decimal` | Mayor a `0` | ✅ |
| `type` | `string` | `FIADO` \| `PAGO` | ✅ |
| `description` | `string` | Texto libre | ❌ |

**Response `201 Created`:**
```json
{
  "id": 42,
  "amount": 500.00,
  "type": "FIADO",
  "description": "Mercadería del martes",
  "createdAt": "2026-10-25T14:30:00"
}
```

| Código | Descripción |
|---|---|
| `201` | Transacción registrada exitosamente. |
| `404` | El cliente no existe o no pertenece al usuario autenticado. |

---

#### `GET /api/clients/{clientId}/transactions`

Devuelve el resumen completo del cliente: saldo acumulado e historial de movimientos ordenado por fecha descendente.

**Response `200 OK`:**
```json
{
  "clientId": 1,
  "clientName": "María López",
  "totalBalance": 1500.00,
  "transactions": [
    {
      "id": 42,
      "amount": 500.00,
      "type": "FIADO",
      "description": "Mercadería del martes",
      "createdAt": "2026-10-25T14:30:00"
    },
    {
      "id": 39,
      "amount": 200.00,
      "type": "PAGO",
      "description": "Abono parcial",
      "createdAt": "2026-10-20T10:00:00"
    }
  ]
}
```

---

## Reglas de Negocio

### Cálculo de Saldo

El `totalBalance` de un cliente se calcula como:

```
totalBalance = Σ transacciones FIADO − Σ transacciones PAGO
```

Un `totalBalance > 0` significa que el cliente tiene deuda pendiente.
Un `totalBalance <= 0` significa que el cliente está al día o con saldo a favor.

---

### Motor de Puntaje de Riesgo

El `riskLevel` de cada cliente se determina automáticamente mediante `RiskScoreService` según las siguientes reglas:

| Condición | Nivel de Riesgo |
|---|---|
| `totalBalance <= 0` (sin deuda o al día) | `BAJO` 🟢 |
| `totalBalance > $50.000` | `ALTO` 🔴 |
| Última transacción hace **más de 30 días** | `ALTO` 🔴 |
| `totalBalance > 0` y no cumple condición ALTO | `MEDIO` 🟡 |

El nivel de riesgo se incluye en todas las respuestas de cliente (`GET /api/clients` y `GET /api/clients/{id}`).

---

## Pruebas

El proyecto incluye **23 tests unitarios** con JUnit 5 + Mockito + AssertJ.

```bash
# Ejecutar todos los tests
./mvnw clean test

# Con reporte detallado
./mvnw clean test -Dsurefire.failIfNoSpecifiedTests=false
```

**Suites disponibles:**

| Archivo | Tests | Descripción |
|---|---|---|
| `RiskScoreServiceTest` | 5 | Reglas del motor de riesgo |
| `ClientServiceTest` | 6 | Lógica de clientes, saldo y deudores |
| `ClientControllerTest` | 4 | Endpoints MockMvc de clientes |
| `TransactionServiceTest` | 5 | Registro y validación de transacciones |
| `TransactionControllerTest` | 3 | Endpoints MockMvc de transacciones |

---

## Roadmap

| Sprint | Estado | Descripción |
|---|---|---|
| Sprint 1 — Setup & Auth + Clientes | ✅ Completo | Estructura, JWT, CRUD de clientes |
| Sprint 2 — Fiados y Pagos | ✅ Completo | Transacciones, saldo y deudores |
| Sprint 3 — Scoring | ✅ Completo | Motor de riesgo + tests unitarios |
| Colchón — Ajustes y Entrega | ✅ Completo | README final y ajustes pre-demo |

Ver [ROADMAP.md](./ROADMAP.md) para el detalle completo de tareas.

---

## Licencia

Este proyecto fue desarrollado con fines académicos y de demostración.