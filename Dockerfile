FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests -q

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=builder /build/target/payment-api-*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
