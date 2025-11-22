# Build stage
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /app

# Copy Maven files
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline

# Copy source and build
COPY src ./src
RUN ./mvnw clean package -DskipTests
RUN java -Djarmode=layertools -jar target/*.jar extract --destination target/extracted

# Runtime stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Create non-root user
RUN useradd -m -u 1001 appuser

# Copy layers for better caching
COPY --from=builder --chown=appuser:appuser /app/target/extracted/dependencies/ ./
COPY --from=builder --chown=appuser:appuser /app/target/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=appuser:appuser /app/target/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=appuser:appuser /app/target/extracted/application/ ./

USER appuser
EXPOSE 8082

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]