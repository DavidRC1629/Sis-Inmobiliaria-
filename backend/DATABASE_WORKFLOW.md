# 📋 Guía de Trabajo con Base de Datos

## 🚀 Cómo Funciona el Sistema

### **Flyway + Spring Boot = Magia Automática**

Cuando ejecutas la aplicación:
1. Spring Boot se conecta a MySQL
2. Flyway revisa qué migraciones faltan
3. Ejecuta automáticamente los scripts SQL nuevos
4. Registra en tabla `flyway_schema_history` lo que ya aplicó

**NO necesitas ejecutar nada manualmente en DBeaver** ✅

---

## 📂 Estructura de Migraciones

```
src/main/resources/db/migration/
├── V1__Initial_schema.sql         ✅ Creado (roles + users)
├── V2__Add_properties_table.sql   📝 Siguiente que crearé
├── V3__Add_clients_table.sql      📝 Y así sucesivamente...
└── V4__...
```

### **Reglas de Nombres:**
- `V1`, `V2`, `V3`... = Número de versión
- `__` = Doble guión bajo (obligatorio)
- Descripción en inglés con guiones bajos
- Extensión `.sql`

---

## 🔄 Flujo de Trabajo

### **TÚ me dices:**
> "Agrega una tabla de propiedades con campos: título, descripción, precio, dirección"

### **YO hago:**
1. Creo `V2__Add_properties_table.sql`
2. Escribo el SQL con CREATE TABLE + índices
3. Te confirmo que está listo

### **TÚ ejecutas:**
```bash
mvn spring-boot:run
```

### **Spring Boot hace:**
✅ Lee el script V2
✅ Lo ejecuta en MySQL
✅ Crea la tabla automáticamente
✅ Registra que V2 ya está aplicado

---

## 🛠️ Comandos Útiles

### **Ver estado de migraciones:**
```bash
mvn flyway:info
```

### **Limpiar base de datos (¡CUIDADO!):**
```bash
mvn flyway:clean  # Borra TODA la base de datos
```

### **Aplicar migraciones manualmente (opcional):**
```bash
mvn flyway:migrate
```

---

## ⚠️ Reglas Importantes

### **✅ SI puedes hacer:**
- Crear nuevas tablas (V2, V3, V4...)
- Agregar columnas a tablas existentes
- Crear índices
- Insertar datos iniciales

### **❌ NO hagas:**
- Modificar scripts ya ejecutados (V1, V2 anteriores)
- Borrar scripts de migración
- Cambiar nombres de archivos existentes

### **Si necesitas corregir algo:**
Crea una NUEVA migración:
```sql
-- V5__Fix_column_name.sql
ALTER TABLE propiedades CHANGE direccion direccion_completa VARCHAR(500);
```

---

## 🎯 Estado Actual

### **✅ Migración V1 Creada:**
- ✅ Tabla `roles` (ROLE_ADMIN, ROLE_USER)
- ✅ Tabla `users` (con usuario admin)
- ✅ Índices optimizados
- ✅ Datos iniciales

### **🔜 Próximas Migraciones:**
Dime qué tablas necesitas y las creo:
- Propiedades/Inmuebles
- Clientes
- Transacciones
- Pagos
- etc.

---

## 🔍 Verificar en DBeaver

Después de ejecutar la app, verás en MySQL:
```sql
-- Ver historial de migraciones
SELECT * FROM flyway_schema_history;

-- Ver tablas creadas
SHOW TABLES;

-- Ver datos
SELECT * FROM roles;
SELECT * FROM users;
```

---

## 💡 Ventajas de Flyway

✅ **Control de versiones** de la base de datos
✅ **Automático**: No ejecutas SQL manualmente
✅ **Reproducible**: Mismo esquema en dev/prod
✅ **Seguro**: No borra datos existentes
✅ **Auditable**: Historial completo de cambios

---

## 🚀 ¡Empecemos!

**Dime qué tablas necesitas para tu sistema inmobiliario y las creo en V2, V3, etc.**

Ejemplos comunes:
- Propiedades/Inmuebles
- Clientes/Compradores
- Agentes inmobiliarios
- Citas/Visitas
- Contratos
- Pagos
- Imágenes de propiedades
- etc.
