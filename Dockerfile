# Build stage
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Run stage
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
COPY fix-jdbc-url.sh /
COPY keep-alive.sh /
COPY db-init.sh /
RUN chmod +x /fix-jdbc-url.sh /keep-alive.sh /db-init.sh
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
EXPOSE 8080
ENTRYPOINT ["/keep-alive.sh", "/fix-jdbc-url.sh", "/db-init.sh"]
CMD ["java", "-Dspring.profiles.active=prod", "-Dserver.port=${PORT:8080}", "-jar", "app.jar"] 