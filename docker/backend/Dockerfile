FROM maven:3.8-openjdk-8 AS builder

WORKDIR /app

COPY pom.xml .
COPY power-trade-rag-common/pom.xml power-trade-rag-common/
COPY power-trade-rag-dal/pom.xml power-trade-rag-dal/
COPY power-trade-rag-core/pom.xml power-trade-rag-core/
COPY power-trade-rag-api/pom.xml power-trade-rag-api/

RUN mvn dependency:go-offline -B

COPY power-trade-rag-common/src power-trade-rag-common/src
COPY power-trade-rag-dal/src power-trade-rag-dal/src
COPY power-trade-rag-core/src power-trade-rag-core/src
COPY power-trade-rag-api/src power-trade-rag-api/src

RUN mvn clean package -DskipTests

FROM openjdk:8-jre-slim

WORKDIR /app

COPY --from=builder /app/power-trade-rag-api/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]