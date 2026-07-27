FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copiar el pom.xml primero para aprovechar la caché de capas de Docker
COPY pom.xml .
# Descargar dependencias con mvn nativo
RUN mvn dependency:go-offline -B -q

# Copiar código fuente y compilar
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime
FROM eclipse-temurin:21-jre
WORKDIR /app

# curl para el healthcheck del contenedor (ver docker-compose.yml)
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

# Usuario no-root: el contenedor corre la JVM como spring:spring
RUN addgroup --system spring && adduser --system --ingroup spring spring
COPY --from=build --chown=spring:spring /app/target/*.jar app.jar
USER spring:spring

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]