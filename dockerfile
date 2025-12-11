# Etapa de construcción
FROM ubuntu:latest AS build
RUN apt-get update
RUN apt-get install -y openjdk-17-jdk maven
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:resolve
COPY . .
RUN mvn -B clean package -DskipTests

# Imagen final más ligera
FROM eclipse-temurin:17-jdk-jammy
EXPOSE 8080
# Allow runtime override of JVM options via JAVA_OPTS env var
ENV JAVA_OPTS=""
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar app.jar"]