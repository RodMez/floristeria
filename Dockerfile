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
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]