
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

# Install ca-certificates utilities
RUN apt-get update && apt-get install -y --no-install-recommends ca-certificates && rm -rf /var/lib/apt/lists/*

# Copy the full-chain certificate bundle
COPY tekniker.crt /usr/local/share/ca-certificates/tekniker.crt

# Update OS store AND explicitly load into Java's cacerts keystore
RUN update-ca-certificates && \
    keytool -importcert -trustcacerts \
      -file /usr/local/share/ca-certificates/tekniker.crt \
      -alias tekniker-chain-2026 \
      -cacerts \
      -storepass changeit \
      -noprompt

EXPOSE 8090

CMD ["java", "-jar", "app.jar"]