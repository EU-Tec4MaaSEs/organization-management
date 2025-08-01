
FROM maven:3-openjdk-17 AS build

WORKDIR /app

COPY pom.xml ./
COPY src ./src
RUN ls /app/src/main/resources
RUN mvn clean package -DskipTests

FROM openjdk:17-jdk-slim

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8090

CMD ["java", "-jar", "app.jar"]