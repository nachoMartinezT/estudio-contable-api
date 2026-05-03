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
COPY document-service document-service/
RUN mvn install -f shared/pom.xml -DskipTests -q && mvn package -f auth-service/pom.xml -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/auth-service/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
