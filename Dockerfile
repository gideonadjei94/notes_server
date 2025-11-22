
# Build Stage
FROM eclipse-temurin:21-jdk-jammy AS builder


WORKDIR /app

COPY mvnw pom.xml ./
COPY .mvn/ .mvn/

RUN chmod +x mvnw

RUN ./mvnw dependency:go-offline

COPY src ./src

RUN ./mvnw clean package -DskipTests

RUN java -Djarmode=layertools -jar target/*.jar extract --destination target/extracted



# Runtime Stage
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

RUN useradd -m -u 1001 appuser

COPY --from=builder --chown=appuser:appuser /app/target/extracted/dependencies/ ./
COPY --from=builder --chown=appuser:appuser /app/target/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=appuser:appuser /app/target/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=appuser:appuser /app/target/extracted/application/ ./

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
