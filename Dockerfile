# Etapa 1: compila el jar con Gradle (sin correr los tests, Render solo
# necesita el artefacto final; los tests ya corren en CI/local).
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN chmod +x gradlew && ./gradlew clean build -x test --no-daemon

# Etapa 2: imagen final liviana, solo el JRE y el jar ya compilado.
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/prueba-1.0.jar app.jar

# Render inyecta PORT; application.properties ya lee server.port=${PORT:8081}.
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
