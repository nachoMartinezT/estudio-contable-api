FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY shared shared/
COPY api-gateway api-gateway/
COPY auth-service auth-service/
COPY client-service client-service/
COPY invoice-service invoice-service/
COPY afip-service afip-service/
COPY audit-service audit-service/
COPY dashboard-service dashboard-service/
RUN mvn -pl shared -am install -DskipTests -q && mvn -pl auth-service -am package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/auth-service/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
