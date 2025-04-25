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
RUN chmod +x /fix-jdbc-url.sh
EXPOSE 8080
ENTRYPOINT ["/fix-jdbc-url.sh"]
CMD ["java", "-jar", "app.jar"] 