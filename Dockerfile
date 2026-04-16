# Étape 1 : Construction (Build) avec Gradle
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /workspace/app

# Copier les fichiers gradle
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Rendre gradlew exécutable
RUN chmod +x gradlew

# Copier le code source
COPY src src

# Construire l'application (le jar) sans exécuter les tests pour gagner du temps en prod
RUN ./gradlew bootJar -x test --no-daemon

# Étape 2 : Exécution (Runtime) allégée
FROM eclipse-temurin:25-jre
WORKDIR /app

# Copier uniquement le jar généré depuis l'étape précédente
COPY --from=builder /workspace/app/build/libs/*.jar app.jar

EXPOSE 8080
VOLUME ["/app/data"]

ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar /app/app.jar"]
