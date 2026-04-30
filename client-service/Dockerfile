FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY shared shared/
COPY client-service client-service/
RUN apk add --no-cache maven && mvn -pl shared -am clean install -DskipTests -q && mvn -pl client-service -am clean package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/client-service/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
