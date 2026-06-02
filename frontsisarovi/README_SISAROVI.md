# SisArovi Frontend - Sistema Inmobiliario

Frontend desarrollado con Angular 17+ (Standalone Components) para el sistema inmobiliario SisArovi.

## 🚀 Tecnologías

- **Angular 20** (CLI 20.3.9)
- **Node.js 22.20.0**
- **Standalone Components** (sin módulos)
- **Zoneless** (sin zone.js para mejor rendimiento)
- **Server-Side Rendering (SSR)**
- **JWT Authentication**

## 📁 Estructura del Proyecto

```
frontsisarovi/
├── src/
│   ├── app/
│   │   ├── guards/              # Guards de autenticación
│   │   │   └── auth.guard.ts
│   │   ├── interceptors/        # Interceptores HTTP
│   │   │   └── auth.interceptor.ts
│   │   ├── models/              # Interfaces y DTOs
│   │   │   └── user.model.ts
│   │   ├── pages/               # Páginas/Componentes
│   │   │   ├── login/
│   │   │   └── dashboard/
│   │   ├── services/            # Servicios
│   │   │   └── auth.service.ts
│   │   ├── app.ts              # Componente raíz
│   │   ├── app.config.ts       # Configuración de la app
│   │   └── app.routes.ts       # Definición de rutas
│   ├── index.html
│   ├── main.ts
│   └── styles.css
```

## 🔧 Instalación y Ejecución

### 1. Instalar dependencias

```bash
cd C:\Users\david\Videos\frontsisarovi
npm install
```

### 2. Iniciar el servidor de desarrollo

```bash
ng serve
```

La aplicación estará disponible en: **http://localhost:4200**

## 🔐 Autenticación

### Login por Defecto

- **Usuario:** `admin`
- **Contraseña:** `admin123`
- **Rol:** ROLE_ADMIN

### Flujo de Autenticación

1. El usuario ingresa credenciales en `/login`
2. El `AuthService` envía la petición al backend (`POST /api/auth/login`)
3. El backend valida y retorna un JWT token
4. El token se guarda en `localStorage`
5. El `authInterceptor` agrega el token a todas las peticiones HTTP
6. El `authGuard` protege las rutas que requieren autenticación

## 🛠️ Características Implementadas

### ✅ Autenticación JWT
- Login y logout
- Interceptor automático para agregar token a peticiones
- Guard para proteger rutas
- Manejo de roles (ADMIN/USER)

### ✅ Páginas
- **Login:** Formulario de inicio de sesión
- **Dashboard:** Panel principal con información del usuario

### ✅ Servicios
- **AuthService:** Gestión de autenticación y usuario actual
- Observable `currentUser$` para reactividad

### ✅ Seguridad
- Guards para rutas protegidas
- Verificación de roles
- Manejo de tokens JWT

## 🌐 Integración con Backend

### API Backend
**URL Base:** `http://localhost:8080`

### Endpoints Utilizados

```
POST /api/auth/login
POST /api/auth/register
```

### CORS
El backend está configurado para aceptar peticiones desde `http://localhost:4200`

## 📝 Comandos Útiles

```bash
# Desarrollo
ng serve                    # Iniciar servidor de desarrollo
ng serve --open            # Abrir automáticamente en navegador

# Build
ng build                   # Build para producción
ng build --configuration development  # Build desarrollo

# Tests
ng test                    # Ejecutar tests unitarios
ng e2e                     # Ejecutar tests e2e

# Generación de componentes
ng generate component pages/users
ng generate service services/property
ng generate guard guards/admin
```

## 🎨 Estilos

Los componentes utilizan estilos con gradientes modernos:
- **Color primario:** `#667eea`
- **Color secundario:** `#764ba2`
- **Gradiente:** `linear-gradient(135deg, #667eea 0%, #764ba2 100%)`

## 🔄 Próximas Funcionalidades

- [ ] Gestión de usuarios (CRUD)
- [ ] Gestión de propiedades inmobiliarias
- [ ] Sistema de roles avanzado
- [ ] Dashboard con estadísticas
- [ ] Búsqueda y filtros de propiedades
- [ ] Carga de imágenes
- [ ] Reportes en PDF

## 📱 Modo Desarrollo

```bash
# Terminal 1 - Backend (Spring Boot)
cd C:\Users\david\Videos\SisArovi\backend
mvn spring-boot:run

# Terminal 2 - Frontend (Angular)
cd C:\Users\david\Videos\frontsisarovi
ng serve
```

## 🐛 Troubleshooting

### Error de CORS
Si aparecen errores de CORS, verificar que el backend esté corriendo en puerto 8080 y que `SecurityConfig.java` tenga:
```java
configuration.setAllowedOrigins(List.of("http://localhost:4200"));
```

### Token no enviado
Verificar que el `authInterceptor` esté configurado en `app.config.ts`:
```typescript
provideHttpClient(withInterceptors([authInterceptor]))
```

### Rutas no protegidas
Asegurarse de que las rutas tengan el guard configurado:
```typescript
{
  path: 'dashboard',
  canActivate: [authGuard],
  ...
}
```

## 📄 Licencia

Proyecto privado - SisArovi © 2025
