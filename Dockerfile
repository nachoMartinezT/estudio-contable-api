# 1. Usamos Eclipse Temurin (la distribución estándar actual de OpenJDK) versión 21 sobre Alpine Linux (muy ligera)
FROM eclipse-temurin:21-jdk-alpine

# 2. Creamos una carpeta de trabajo interna
WORKDIR /app

# 3. Copiamos el archivo JAR generado por Maven
COPY target/*.jar app.jar

# 4. Exponemos el puerto 8080
EXPOSE 8080

# 5. Comando para arrancar la app
ENTRYPOINT ["java", "-jar", "app.jar"]