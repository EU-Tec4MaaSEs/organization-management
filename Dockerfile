
FROM maven:3-openjdk-17 AS build

WORKDIR /app

COPY pom.xml ./
COPY src ./src
RUN ls /app/src/main/resources
RUN mvn clean package -DskipTests

FROM openjdk:17.0.2-jdk-slim-bullseye

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

USER root

COPY tekniker.crt /usr/local/share/ca-certificates/tekniker.crt

RUN keytool -importcert -trustcacerts \
    -file /usr/local/share/ca-certificates/tekniker.crt \
    -alias tekniker-chain \
    -keystore $JAVA_HOME/lib/security/cacerts \
    -storepass changeit \
    -noprompt


EXPOSE 8090

CMD ["java", "-jar", "app.jar"]