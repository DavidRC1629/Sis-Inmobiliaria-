# SisArovi - Sistema Inmobiliario Full Stack

Sistema completo con **Spring Boot 3** (Backend) + **Angular 17+** (Frontend)

## 📁 Estructura del Proyecto

```
SisArovi/
├── backend/                    # Spring Boot API
│   ├── src/main/java/
│   ├── src/main/resources/
│   └── pom.xml
├── frontsisarovi/              # Angular Frontend
│   ├── src/app/
│   ├── package.json
│   └── README_SISAROVI.md
└── START-FULLSTACK.bat         # Script de inicio
```

## 🚀 INICIO RÁPIDO

### ⚠️ REQUISITO PREVIO

**Ejecuta PRIMERO en MySQL Workbench:**

```sql
DROP DATABASE IF EXISTS sisarovi_db;
CREATE DATABASE sisarovi_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### Luego ejecuta:

```bash
START-FULLSTACK.bat
```

## 🔐 Credenciales

- **Usuario:** `admin`
- **Password:** `admin123`
- **Rol:** ROLE_ADMIN

## 🌐 URLs

- **Backend API:** http://localhost:8080
- **Frontend:** http://localhost:4200
- **H2 Console:** http://localhost:8080/h2-console (dev)

## 🛠️ Stack Tecnológico

### Backend
- Java 21 LTS
- Spring Boot 3.2.1
- Spring Security + JWT
- MySQL 8.0
- Flyway Migrations
- Lombok

### Frontend
- Angular 20
- Standalone Components
- TypeScript
- HttpClient + JWT Interceptor
- Guards + Routing

## 📊 Base de Datos

**MySQL - sisarovi_db**

Tablas:
- `roles` (id, name)
- `users` (id, username, password, first_name, last_name, email, role_id, enabled)
- `flyway_schema_history` (control de migraciones)

## 🔗 Endpoints API

### Autenticación
```
POST /api/auth/login      - Login con JWT
POST /api/auth/register   - Registro de usuarios
```

## 📝 Desarrollo

### Iniciar Backend solo
```bash
cd backend
mvn spring-boot:run
```

### Iniciar Frontend solo
```bash
cd frontsisarovi
ng serve
```

## 🎯 Flujo de Trabajo

1. **Login:** Usuario ingresa credenciales en Angular
2. **API Call:** Frontend hace POST a `/api/auth/login`
3. **JWT:** Backend valida y retorna token
4. **Storage:** Token se guarda en localStorage
5. **Interceptor:** Todas las peticiones incluyen `Authorization: Bearer {token}`
6. **CRUD:** Operaciones se mapean automáticamente a MySQL

## 🐛 Troubleshooting

### Backend no inicia
```bash
# Verifica MySQL
Get-Service MySQL80,MySQL81

# Limpia y recompila
cd backend
mvn clean package -DskipTests
```

### Error de migración Flyway
```sql
-- Ejecuta en MySQL Workbench
DROP DATABASE IF EXISTS sisarovi_db;
CREATE DATABASE sisarovi_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### Frontend no conecta
- Verifica que backend esté en puerto 8080
- Revisa CORS en `SecurityConfig.java`
- Debe permitir: `http://localhost:4200`

## 📅 Changelog

- **2025-12-29**: Creación proyecto completo
- **2025-12-29**: Upgrade Java 21 + MySQL + Flyway
- **2025-12-29**: Frontend Angular con JWT auth
- **2025-12-29**: Integración completa Backend-Frontend

---

© 2025 SisArovi - Sistema Inmobiliario
