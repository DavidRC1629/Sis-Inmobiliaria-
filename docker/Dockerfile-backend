# ====== Stage 1: Build ======
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copiar pom primero para aprovechar la caché de dependencias
COPY pom.xml .
RUN mvn dependency:go-offline

# Copiar el código fuente y compilar
COPY src ./src
RUN mvn clean package -DskipTests

# ====== Stage 2: Run ======
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copiar el archivo .jar generado en la etapa anterior (Stage 1)
# Se usa 'target/*.jar' para capturar cualquier nombre de archivo generado
COPY --from=builder /app/target/*.jar app.jar

# Puerto por defecto de Spring Boot
EXPOSE 8080

# Ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]
