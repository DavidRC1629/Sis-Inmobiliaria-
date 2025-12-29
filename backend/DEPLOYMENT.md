# 📦 Guía de Despliegue - Sistema Inmobiliario

## 🏠 Desarrollo (Tu Máquina)

### Ejecutar en modo desarrollo:
```bash
mvn spring-boot:run
```
o
```bash
java -jar inmobiliario-backend.jar
```

Usa automáticamente: `application-dev.properties` (localhost:3306)

---

## 🚀 Producción (Servidor Cliente)

### PASO 1: Configurar Servidor MySQL del Cliente

Editar: `src/main/resources/application-prod.properties`

```properties
# Cambiar:
spring.datasource.url=jdbc:mysql://192.168.1.100:3306/sisarovi_db
spring.datasource.username=sisarovi_user
spring.datasource.password=ContraseñaSegura123!

# Generar nuevo secret JWT (256 bits):
jwt.secret=NuevoSecretAleatorioSeguroMinimo256BitsParaProduccion
```

### PASO 2: Compilar para Producción

```bash
mvn clean package -DskipTests
```

Genera: `target/inmobiliario-backend-1.0.0.jar`

### PASO 3: Ejecutar en Producción

```bash
java -jar inmobiliario-backend-1.0.0.jar --spring.profiles.active=prod
```

o con variable de entorno:

```bash
export SPRING_PROFILES_ACTIVE=prod
java -jar inmobiliario-backend-1.0.0.jar
```

---

## 🔄 Cambiar entre Ambientes

### Opción 1: Parámetro al ejecutar
```bash
# Desarrollo
java -jar app.jar --spring.profiles.active=dev

# Producción
java -jar app.jar --spring.profiles.active=prod
```

### Opción 2: Variable de entorno
```bash
# Windows
set SPRING_PROFILES_ACTIVE=prod

# Linux/Mac
export SPRING_PROFILES_ACTIVE=prod
```

### Opción 3: application.properties
```properties
spring.profiles.active=dev  # Cambiar a 'prod' para producción
```

---

## ✅ Ventajas de esta Configuración

✔️ **Mismo código** para desarrollo y producción
✔️ **No modificas nada** al entregar al cliente
✔️ **Seguro**: Producción sin logs sensibles
✔️ **Flexible**: Cambias ambiente con un parámetro

---

## 🗄️ Preparar Base de Datos del Cliente

Antes de ejecutar en producción, crear la BD en el servidor:

```sql
-- En MySQL del servidor cliente
CREATE DATABASE sisarovi_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'sisarovi_user'@'%' IDENTIFIED BY 'ContraseñaSegura123!';
GRANT ALL PRIVILEGES ON sisarovi_db.* TO 'sisarovi_user'@'%';
FLUSH PRIVILEGES;
```

Spring Boot creará las tablas automáticamente al iniciar.

---

## 📋 Checklist Pre-Entrega

- [ ] Actualizar `application-prod.properties` con datos del cliente
- [ ] Cambiar JWT secret en producción
- [ ] Crear base de datos en servidor del cliente
- [ ] Compilar: `mvn clean package`
- [ ] Probar localmente con perfil prod
- [ ] Entregar JAR + instrucciones
- [ ] Documentar usuario admin (admin/admin123)
