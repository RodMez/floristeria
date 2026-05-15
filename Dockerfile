FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copiar archivos de Maven primero
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# Descargar dependencias y cachearlas
RUN ./mvnw dependency:go-offline

# Ahora copiar el código fuente
COPY src ./src

# Compilar
RUN ./mvnw clean package -DskipTests

# Runtime
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]