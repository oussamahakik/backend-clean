FROM eclipse-temurin:21-jre

WORKDIR /app

# Le jar est produit par ./gradlew bootJar
COPY build/libs/*.jar app.jar

EXPOSE 8080
VOLUME ["/app/data"]

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
